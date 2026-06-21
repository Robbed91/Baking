package com.robbiebedford.bakebook.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import com.robbiebedford.bakebook.data.database.PhotoEntryEntity
import com.robbiebedford.bakebook.data.database.RecipeCategory
import com.robbiebedford.bakebook.data.database.RecipeEntity
import com.robbiebedford.bakebook.data.database.RecipeLinkEntity
import com.robbiebedford.bakebook.data.database.RecipeWithDetails
import com.robbiebedford.bakebook.data.database.ShoppingItemEntity
import com.robbiebedford.bakebook.ui.theme.Cream
import com.robbiebedford.bakebook.ui.theme.Orange
import com.robbiebedford.bakebook.ui.theme.SoftCard
import com.robbiebedford.bakebook.viewmodels.BakeBookViewModel
import com.robbiebedford.bakebook.viewmodels.RecipeForm
import com.robbiebedford.bakebook.viewmodels.RecipeSort
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.DateFormat
import kotlin.math.max

@Composable
fun RecipeScreen(viewModel: BakeBookViewModel, onOpen: (Long) -> Unit) {
    val recipes by viewModel.recipes.collectAsState()
    var search by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("All") }
    var sort by remember { mutableStateOf(RecipeSort.Date) }
    var showForm by remember { mutableStateOf(false) }
    val visible = recipes
        .filter { it.title.contains(search, true) || it.category.contains(search, true) }
        .filter { category == "All" || it.category == category }
        .let {
            when (sort) {
                RecipeSort.Name -> it.sortedBy { recipe -> recipe.title }
                RecipeSort.Date -> it.sortedByDescending { recipe -> recipe.dateUpdated }
                RecipeSort.Rating -> it.sortedByDescending { recipe -> recipe.rating }
                RecipeSort.Category -> it.sortedBy { recipe -> recipe.category }
            }
        }

    Scaffold(floatingActionButton = { FloatingActionButton(onClick = { showForm = true }) { Text("+") } }) { padding ->
        LazyColumn(Modifier.fillMaxSize().background(Cream).padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text("Recipes", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                SearchAndFilters(search, { search = it }, category, { category = it }, sort, { sort = it })
            }
            if (visible.isEmpty()) item { EmptyCard("No recipes yet. Add your first bake.") }
            items(visible, key = { it.id }) { recipe ->
                RecipeCard(recipe, onOpen, onFavourite = { viewModel.toggleFavourite(recipe) }, onDuplicate = { viewModel.duplicateRecipe(recipe.id) }, onDelete = { viewModel.deleteRecipe(recipe.id) })
            }
        }
    }
    if (showForm) RecipeFormDialog(onDismiss = { showForm = false }, onSave = {
        viewModel.saveRecipe(it) { message -> showForm = false }
    })
}

@Composable
private fun SearchAndFilters(search: String, onSearch: (String) -> Unit, category: String, onCategory: (String) -> Unit, sort: RecipeSort, onSort: (RecipeSort) -> Unit) {
    OutlinedTextField(value = search, onValueChange = onSearch, label = { Text("Search") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        MenuButton(label = category, options = listOf("All") + RecipeCategory.entries.map { it.name }, onSelected = onCategory)
        MenuButton(label = sort.name, options = RecipeSort.entries.map { it.name }, onSelected = { onSort(RecipeSort.valueOf(it)) })
    }
}

@Composable
private fun RecipeCard(recipe: RecipeEntity, onOpen: (Long) -> Unit, onFavourite: () -> Unit, onDuplicate: () -> Unit, onDelete: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = SoftCard), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().clickable { onOpen(recipe.id) }) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(recipe.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = onFavourite) { Text(if (recipe.favourite) "★" else "☆") }
            }
            Text("${recipe.category} • Prep ${recipe.prepTime.ifBlank { "-" }} • Bake ${recipe.bakeTime.ifBlank { "-" }} • ${recipe.ovenTemperature.ifBlank { "-" }}")
            Text("Serves ${recipe.servings.ifBlank { "-" }} • ${"★".repeat(recipe.rating.coerceIn(0, 5))}${"☆".repeat(5 - recipe.rating.coerceIn(0, 5))}")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = onDuplicate, label = { Text("Duplicate") })
                AssistChip(onClick = onDelete, label = { Text("Delete") })
            }
        }
    }
}

@Composable
private fun RecipeFormDialog(onDismiss: () -> Unit, onSave: (RecipeForm) -> Unit) {
    var form by remember { mutableStateOf(RecipeForm()) }
    var ingredients by remember { mutableStateOf(listOf("")) }
    var methodSteps by remember { mutableStateOf(listOf("")) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Recipe") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Field("Title", form.title) { form = form.copy(title = it) } }
                item { MenuButton(form.category, RecipeCategory.entries.map { it.name }) { form = form.copy(category = it) } }
                item { Field("Prep Time", form.prepTime) { form = form.copy(prepTime = it) } }
                item { Field("Bake Time", form.bakeTime) { form = form.copy(bakeTime = it) } }
                item { Field("Oven Temperature", form.ovenTemperature) { form = form.copy(ovenTemperature = it) } }
                item { Field("Servings", form.servings) { form = form.copy(servings = it) } }
                item {
                    DynamicTextRows(
                        title = "Ingredients (oz)",
                        addLabel = "Add ingredient",
                        itemLabel = { index -> "Ingredient ${index + 1}" },
                        values = ingredients,
                        onValues = { ingredients = it }
                    )
                }
                item {
                    DynamicTextRows(
                        title = "Method",
                        addLabel = "Add step",
                        itemLabel = { index -> "Step ${index + 1}" },
                        values = methodSteps,
                        onValues = { methodSteps = it }
                    )
                }
                item { Field("Notes", form.notes, true) { form = form.copy(notes = it) } }
                item {
                    Text("Rating: ${form.rating}")
                    Slider(value = form.rating.toFloat(), onValueChange = { form = form.copy(rating = it.toInt()) }, valueRange = 0f..5f, steps = 4)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(form.copy(ingredients = ingredients.joinToString("\n"), method = methodSteps.joinToString("\n")))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun DynamicTextRows(
    title: String,
    addLabel: String,
    itemLabel: (Int) -> String,
    values: List<String>,
    onValues: (List<String>) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Button(onClick = { onValues(values + "") }) { Text(addLabel) }
        }
        values.forEachIndexed { index, value ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = value,
                    onValueChange = { updated ->
                        onValues(values.toMutableList().also { it[index] = updated })
                    },
                    label = { Text(itemLabel(index)) },
                    modifier = Modifier.weight(1f),
                    minLines = if (title == "Method") 2 else 1
                )
                if (values.size > 1) {
                    TextButton(onClick = { onValues(values.toMutableList().also { it.removeAt(index) }) }) {
                        Text("Remove")
                    }
                }
            }
        }
    }
}

@Composable
fun RecipeDetailScreen(viewModel: BakeBookViewModel, recipeId: Long) {
    val context = LocalContext.current
    var details by remember { mutableStateOf<RecipeWithDetails?>(null) }
    var scale by remember { mutableFloatStateOf(1f) }
    LaunchedEffect(recipeId) { viewModel.recipeDetails(recipeId) { details = it } }
    val current = details
    if (current == null) {
        EmptyCard("Recipe not found.")
        return
    }
    LazyColumn(Modifier.fillMaxSize().background(Cream).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(current.recipe.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("${current.recipe.category} • Rating ${current.recipe.rating}/5 • Favourite ${if (current.recipe.favourite) "Yes" else "No"}")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                Button(onClick = { shareText(context, recipeAsText(current, scale)) }) { Text("Share") }
                Button(onClick = { exportPdf(context, current, scale) }) { Text("Export PDF") }
                Button(onClick = { viewModel.generateShoppingList(recipeId); toast(context, "Shopping list generated.") }) { Text("Shopping List") }
            }
        }
        item { ScaleControls(scale) { scale = it } }
        item { Section("Ingredients (oz)", current.ingredients.joinToString("\n") { scaleIngredient(it.text, scale) }) }
        item { Section("Method", current.steps.mapIndexed { i, step -> "${i + 1}. ${step.text}" }.joinToString("\n")) }
        item { Section("Notes", current.recipe.notes.ifBlank { "No notes." }) }
    }
}

@Composable
private fun ScaleControls(scale: Float, onScale: (Float) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = scale == .5f, onClick = { onScale(.5f) }, label = { Text("Half Recipe") })
        FilterChip(selected = scale == 1f, onClick = { onScale(1f) }, label = { Text("Normal Recipe") })
        FilterChip(selected = scale == 2f, onClick = { onScale(2f) }, label = { Text("Double Recipe") })
    }
}

@Composable
fun LinkScreen(viewModel: BakeBookViewModel) {
    val links by viewModel.links.collectAsState()
    val context = LocalContext.current
    var search by remember { mutableStateOf("") }
    var showForm by remember { mutableStateOf(false) }
    val visible = links.filter { it.title.contains(search, true) || it.websiteName.contains(search, true) || it.category.contains(search, true) }
    Scaffold(floatingActionButton = { FloatingActionButton(onClick = { showForm = true }) { Text("+") } }) { padding ->
        LazyColumn(Modifier.fillMaxSize().background(Cream).padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("Saved Links", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Field("Search", search) { search = it } }
            if (visible.isEmpty()) item { EmptyCard("No recipe links saved.") }
            items(visible, key = { it.id }) { link ->
                Card(colors = CardDefaults.cardColors(containerColor = SoftCard), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(link.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("${link.websiteName.ifBlank { "Website" }} • ${link.category}")
                        Text(link.url, color = Orange)
                        Text(link.notes)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link.url))) }) { Text("Open") }
                            TextButton(onClick = { viewModel.deleteLink(link.id) }) { Text("Delete") }
                        }
                    }
                }
            }
        }
    }
    if (showForm) LinkFormDialog(onDismiss = { showForm = false }) {
        viewModel.saveLink(it) { message -> toast(context, message); if (message.contains("saved")) showForm = false }
    }
}

@Composable
private fun LinkFormDialog(onDismiss: () -> Unit, onSave: (RecipeLinkEntity) -> Unit) {
    var title by remember { mutableStateOf("") }
    var site by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("https://") }
    var notes by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(RecipeCategory.Other.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Link") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Field("Title", title) { title = it }
                Field("Website Name", site) { site = it }
                Field("URL", url) { url = it }
                MenuButton(category, RecipeCategory.entries.map { it.name }) { category = it }
                Field("Notes", notes, true) { notes = it }
            }
        },
        confirmButton = { Button(onClick = { onSave(RecipeLinkEntity(title = title, websiteName = site, url = url, notes = notes, category = category)) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun PhotoLibraryScreen(viewModel: BakeBookViewModel, onOpen: (Int) -> Unit) {
    val photos by viewModel.photos.collectAsState()
    val recipes by viewModel.recipes.collectAsState()
    val context = LocalContext.current
    var showForm by remember { mutableStateOf(false) }
    Scaffold(floatingActionButton = { FloatingActionButton(onClick = { showForm = true }) { Text("+") } }) { padding ->
        Column(Modifier.fillMaxSize().background(Cream).padding(padding).padding(16.dp)) {
            Text("Photo Library", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            if (photos.isEmpty()) EmptyCard("No bake photos yet.")
            LazyVerticalGrid(columns = GridCells.Adaptive(150.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(photos, key = { _, item -> item.id }) { index, photo ->
                    Card(shape = RoundedCornerShape(8.dp), modifier = Modifier.clickable { onOpen(index) }) {
                        Column {
                            Image(painter = rememberAsyncImagePainter(photo.uri), contentDescription = photo.title, modifier = Modifier.fillMaxWidth().aspectRatio(1f), contentScale = ContentScale.Crop)
                            Column(Modifier.padding(8.dp)) {
                                Text(photo.title, fontWeight = FontWeight.Bold)
                                Text(DateFormat.getDateInstance().format(photo.dateBaked))
                                TextButton(onClick = { viewModel.deletePhoto(photo.id) }) { Text("Delete") }
                            }
                        }
                    }
                }
            }
        }
    }
    if (showForm) PhotoFormDialog(recipes.map { it.id to it.title }, onDismiss = { showForm = false }) {
        viewModel.savePhoto(it) { message -> toast(context, message); if (message.contains("saved")) showForm = false }
    }
}

@Composable
private fun PhotoFormDialog(recipes: List<Pair<Long, String>>, onDismiss: () -> Unit, onSave: (PhotoEntryEntity) -> Unit) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var uri by remember { mutableStateOf("") }
    var linkedId by remember { mutableStateOf<Long?>(null) }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { selected -> if (selected != null) uri = selected.toString() }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) uri = saveBitmapToCache(context, bitmap).toString()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Photo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Field("Title", title) { title = it }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { gallery.launch("image/*") }) { Text("Gallery") }
                    Button(onClick = { camera.launch(null) }) { Text("Camera") }
                }
                Text(if (uri.isBlank()) "No photo selected" else "Photo selected")
                MenuButton(linkedId?.let { id -> recipes.firstOrNull { it.first == id }?.second } ?: "No linked recipe", listOf("No linked recipe") + recipes.map { it.second }) { label ->
                    linkedId = recipes.firstOrNull { it.second == label }?.first
                }
                Field("Notes", notes, true) { notes = it }
            }
        },
        confirmButton = { Button(onClick = { onSave(PhotoEntryEntity(title = title, uri = uri, linkedRecipeId = linkedId, notes = notes)) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun PhotoViewerScreen(viewModel: BakeBookViewModel, index: Int) {
    val photos by viewModel.photos.collectAsState()
    var selected by remember(index, photos.size) { mutableIntStateOf(index.coerceIn(0, max(photos.lastIndex, 0))) }
    val photo = photos.getOrNull(selected)
    var scale by remember { mutableFloatStateOf(1f) }
    val transformState = rememberTransformableState { zoom, _, _ -> scale = (scale * zoom).coerceIn(1f, 5f) }
    Column(Modifier.fillMaxSize().background(Color.Black).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (photo != null) {
            Text(photo.title, color = Color.White, style = MaterialTheme.typography.titleLarge)
            Image(
                painter = rememberAsyncImagePainter(photo.uri),
                contentDescription = photo.title,
                modifier = Modifier.weight(1f).fillMaxWidth().graphicsLayer(scaleX = scale, scaleY = scale).transformable(transformState),
                contentScale = ContentScale.Fit
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(enabled = selected > 0, onClick = { selected-- }) { Text("Previous") }
                Button(enabled = selected < photos.lastIndex, onClick = { selected++ }) { Text("Next") }
            }
        }
    }
}

@Composable
fun ShoppingScreen(viewModel: BakeBookViewModel) {
    val items by viewModel.shoppingItems.collectAsState()
    var name by remember { mutableStateOf("") }
    LazyColumn(Modifier.fillMaxSize().background(Cream).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Shopping List", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Item") }, modifier = Modifier.weight(1f), singleLine = true)
                Spacer(Modifier.width(8.dp))
                Button(onClick = { if (name.isNotBlank()) { viewModel.saveShoppingItem(ShoppingItemEntity(name = name.trim())); name = "" } }) { Text("Add") }
            }
            TextButton(onClick = { viewModel.clearCompletedShopping() }) { Text("Clear completed") }
        }
        items(items, key = { it.id }) { item ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(SoftCard).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = item.complete, onCheckedChange = { viewModel.saveShoppingItem(item.copy(complete = it)) })
                Text(item.name, modifier = Modifier.weight(1f))
                TextButton(onClick = { viewModel.deleteShoppingItem(item.id) }) { Text("Delete") }
            }
        }
    }
}

@Composable
fun TimerScreen(onTimerFinished: (String) -> Unit) {
    Column(Modifier.fillMaxSize().background(Cream).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Baking Timers", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        CountdownClock(title = "Bake Countdown", defaultMinutes = 30, presets = listOf(10, 15, 20, 30, 45, 60), onTimerFinished = onTimerFinished)
        CountdownClock(title = "Cooling Clock", defaultMinutes = 20, presets = listOf(5, 10, 15, 20, 30, 45), onTimerFinished = onTimerFinished)
    }
}

@Composable
private fun CountdownClock(title: String, defaultMinutes: Int, presets: List<Int>, onTimerFinished: (String) -> Unit) {
    var customMinutes by remember { mutableStateOf(defaultMinutes.toString()) }
    var total by remember { mutableLongStateOf(defaultMinutes * 60L) }
    var remaining by remember { mutableLongStateOf(defaultMinutes * 60L) }
    var running by remember { mutableStateOf(false) }
    LaunchedEffect(running, total) {
        while (running && remaining > 0) {
            delay(1000)
            remaining -= 1
        }
        if (running && remaining == 0L) {
            running = false
            onTimerFinished("$title is done.")
        }
    }
    Card(colors = CardDefaults.cardColors(containerColor = SoftCard), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("%02d:%02d".format(remaining / 60, remaining % 60), style = MaterialTheme.typography.displayMedium)
        LinearProgressIndicator(progress = { if (total == 0L) 0f else remaining.toFloat() / total }, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(value = customMinutes, onValueChange = { customMinutes = it.filter(Char::isDigit) }, label = { Text("Minutes") }, modifier = Modifier.weight(1f), singleLine = true)
            Button(onClick = {
                val minutes = customMinutes.toLongOrNull()?.coerceIn(1, 999) ?: defaultMinutes.toLong()
                total = minutes * 60L
                remaining = total
                running = false
            }) { Text("Set") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            presets.forEach { minutes ->
                AssistChip(onClick = { total = minutes * 60L; remaining = total; running = false }, label = { Text("${minutes}m") })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { running = true }) { Text("Start") }
            Button(onClick = { running = false }) { Text("Pause") }
            Button(onClick = { running = false; remaining = total }) { Text("Reset") }
        }
    }
    }
}

@Composable
fun ConverterScreen() {
    var amount by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("oz") }
    var metric by remember { mutableStateOf(true) }
    val result = convertUnit(amount.toFloatOrNull() ?: 0f, unit, metric)
    LazyColumn(Modifier.fillMaxSize().background(Cream).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Unit Calculator", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Switch between imperial baking measures and metric equivalents.")
            Field("Amount", amount) { amount = it }
            MenuButton(unit, listOf("oz", "lb", "fl oz", "cup", "tbsp", "tsp")) { unit = it }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = metric, onClick = { metric = true }, label = { Text("Metric") })
                FilterChip(selected = !metric, onClick = { metric = false }, label = { Text("Imperial") })
            }
            Section("Converted Amount", result)
        }
    }
}

@Composable
fun BackupScreen(viewModel: BakeBookViewModel) {
    val recipes by viewModel.recipes.collectAsState()
    val links by viewModel.links.collectAsState()
    val photos by viewModel.photos.collectAsState()
    val shopping by viewModel.shoppingItems.collectAsState()
    val context = LocalContext.current
    var json by remember { mutableStateOf("") }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
            restoreBackup(json, viewModel, context)
        }
    }
    LazyColumn(Modifier.fillMaxSize().background(Cream).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Backup & Restore", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { json = buildBackupJson(recipes, links, photos, shopping); shareText(context, json) }) { Text("Backup Data") }
                Button(onClick = { restore.launch("application/json") }) { Text("Restore Data") }
            }
            Field("JSON Backup", json, true) { json = it }
            Button(onClick = { restoreBackup(json, viewModel, context) }) { Text("Import JSON") }
        }
    }
}

@Composable
private fun EmptyCard(text: String) {
    Card(colors = CardDefaults.cardColors(containerColor = SoftCard), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Text(text, modifier = Modifier.padding(16.dp))
    }
}

@Composable
private fun Section(title: String, body: String) {
    Card(colors = CardDefaults.cardColors(containerColor = SoftCard), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(body)
        }
    }
}

@Composable
private fun Field(label: String, value: String, multi: Boolean = false, onValue: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValue,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        minLines = if (multi) 3 else 1,
        singleLine = !multi
    )
}

@Composable
private fun MenuButton(label: String, options: List<String>, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        ElevatedButton(onClick = { expanded = true }) { Text(label) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach {
                DropdownMenuItem(text = { Text(it) }, onClick = { expanded = false; onSelected(it) })
            }
        }
    }
}

private fun scaleIngredient(text: String, scale: Float): String {
    val pattern = Regex("""(^|\s)(\d+(\.\d+)?)(?=\s|[a-zA-Z])""")
    return pattern.replace(text) { match ->
        val prefix = match.groupValues[1]
        val value = match.groupValues[2].toFloatOrNull() ?: return@replace match.value
        "$prefix${"%.2f".format(value * scale).trimEnd('0').trimEnd('.')}"
    }
}

private fun recipeAsText(details: RecipeWithDetails, scale: Float): String = buildString {
    appendLine(details.recipe.title)
    appendLine("${details.recipe.category} • Rating ${details.recipe.rating}/5")
    appendLine()
    appendLine("Ingredients")
    details.ingredients.forEach { appendLine("- ${scaleIngredient(it.text, scale)}") }
    appendLine()
    appendLine("Method")
    details.steps.forEachIndexed { index, step -> appendLine("${index + 1}. ${step.text}") }
    appendLine()
    appendLine("Notes")
    appendLine(details.recipe.notes)
}

private fun shareText(context: Context, text: String) {
    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }, "Share with"))
}

private fun exportPdf(context: Context, details: RecipeWithDetails, scale: Float) {
    val document = PdfDocument()
    val page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
    val paint = android.graphics.Paint().apply { textSize = 14f }
    var y = 36f
    recipeAsText(details, scale).lines().forEach { line ->
        page.canvas.drawText(line.take(90), 32f, y, paint)
        y += 20f
        if (y > 800f) return@forEach
    }
    document.finishPage(page)
    val file = File(context.cacheDir, "${details.recipe.title.filter { it.isLetterOrDigit() }}.pdf")
    file.outputStream().use { document.writeTo(it) }
    document.close()
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }, "Export PDF"))
}

private fun saveBitmapToCache(context: Context, bitmap: Bitmap): Uri {
    val file = File(context.cacheDir, "bake_${System.currentTimeMillis()}.jpg")
    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

private fun buildBackupJson(recipes: List<RecipeEntity>, links: List<RecipeLinkEntity>, photos: List<PhotoEntryEntity>, shopping: List<ShoppingItemEntity>): String {
    val root = JSONObject()
    root.put("recipes", JSONArray(recipes.map { JSONObject().put("title", it.title).put("category", it.category).put("notes", it.notes).put("rating", it.rating).put("favourite", it.favourite) }))
    root.put("links", JSONArray(links.map { JSONObject().put("title", it.title).put("websiteName", it.websiteName).put("url", it.url).put("notes", it.notes).put("category", it.category) }))
    root.put("photos", JSONArray(photos.map { JSONObject().put("title", it.title).put("uri", it.uri).put("notes", it.notes).put("dateBaked", it.dateBaked) }))
    root.put("shoppingItems", JSONArray(shopping.map { JSONObject().put("name", it.name).put("complete", it.complete) }))
    return root.toString(2)
}

private fun convertUnit(amount: Float, unit: String, metric: Boolean): String {
    if (amount <= 0f) return "Enter an amount to convert."
    if (!metric) return "%.2f %s".format(amount, unit)
    return when (unit) {
        "oz" -> "%.1f g".format(amount * 28.3495f)
        "lb" -> "%.1f g".format(amount * 453.592f)
        "fl oz" -> "%.1f ml".format(amount * 29.5735f)
        "cup" -> "%.1f ml".format(amount * 240f)
        "tbsp" -> "%.1f ml".format(amount * 14.7868f)
        "tsp" -> "%.1f ml".format(amount * 4.92892f)
        else -> "%.2f %s".format(amount, unit)
    }
}

private fun restoreBackup(json: String, viewModel: BakeBookViewModel, context: Context) {
    runCatching {
        val root = JSONObject(json)
        root.optJSONArray("recipes")?.let { array ->
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                viewModel.saveRecipe(RecipeForm(title = item.optString("title"), category = item.optString("category", RecipeCategory.Other.name), notes = item.optString("notes"), rating = item.optInt("rating"), favourite = item.optBoolean("favourite")))
            }
        }
        root.optJSONArray("links")?.let { array ->
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                viewModel.saveLink(RecipeLinkEntity(title = item.optString("title"), websiteName = item.optString("websiteName"), url = item.optString("url"), notes = item.optString("notes"), category = item.optString("category", RecipeCategory.Other.name)))
            }
        }
        root.optJSONArray("photos")?.let { array ->
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                viewModel.savePhoto(PhotoEntryEntity(title = item.optString("title"), uri = item.optString("uri"), notes = item.optString("notes"), dateBaked = item.optLong("dateBaked", System.currentTimeMillis())))
            }
        }
        root.optJSONArray("shoppingItems")?.let { array ->
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                viewModel.saveShoppingItem(ShoppingItemEntity(name = item.optString("name"), complete = item.optBoolean("complete")))
            }
        }
    }.onSuccess { toast(context, "Backup restored.") }
        .onFailure { toast(context, "Could not restore backup: ${it.message}") }
}

private fun toast(context: Context, message: String) = Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
