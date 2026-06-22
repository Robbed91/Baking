package com.robbiebedford.bakebook.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.app.Activity
import android.view.WindowManager
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
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import com.robbiebedford.bakebook.data.database.AchievementEntity
import com.robbiebedford.bakebook.data.database.BakeLogEntity
import com.robbiebedford.bakebook.data.database.OccasionEntity
import com.robbiebedford.bakebook.data.database.PantryItemEntity
import com.robbiebedford.bakebook.data.database.PhotoEntryEntity
import com.robbiebedford.bakebook.data.database.RecipeCategory
import com.robbiebedford.bakebook.data.database.RecipeCollectionEntity
import com.robbiebedford.bakebook.data.database.RecipeCollectionLinkEntity
import com.robbiebedford.bakebook.data.database.RecipeDifficulty
import com.robbiebedford.bakebook.data.database.RecipeEntity
import com.robbiebedford.bakebook.data.database.RecipeLinkEntity
import com.robbiebedford.bakebook.data.database.RecipeWithDetails
import com.robbiebedford.bakebook.data.database.ShoppingItemEntity
import com.robbiebedford.bakebook.data.database.SubstitutionEntity
import com.robbiebedford.bakebook.data.database.OccasionRecipeLinkEntity
import com.robbiebedford.bakebook.timer.BakeBookTimerScheduler
import com.robbiebedford.bakebook.timer.BakeTimerDefinition
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
import java.util.Calendar
import kotlin.math.max

@Composable
fun HomeDashboardScreen(
    viewModel: BakeBookViewModel,
    onRecipes: () -> Unit,
    onShopping: () -> Unit,
    onTimers: () -> Unit,
    onPhotos: () -> Unit
) {
    val recipes by viewModel.recipes.collectAsState()
    val shopping by viewModel.shoppingItems.collectAsState()
    val bakeLogs by viewModel.bakeLogs.collectAsState()
    val pantry by viewModel.pantryItems.collectAsState()
    val occasions by viewModel.occasions.collectAsState()
    val achievements by viewModel.achievements.collectAsState()
    val context = LocalContext.current
    var showRecipe by remember { mutableStateOf(false) }
    var quickItem by remember { mutableStateOf(false) }

    val recent = recipes.sortedByDescending { it.dateCreated }.take(3)
    val favourites = recipes.filter { it.favourite }.take(3)
    val lastBaked = bakeLogs.maxByOrNull { it.dateBaked }
    val expiring = pantry.filter { (it.expiryDate ?: 0L) > 0L && (it.expiryDate ?: 0L) - System.currentTimeMillis() < 7L * 24L * 60L * 60L * 1000L }
    val nextOccasion = occasions.filter { it.date >= System.currentTimeMillis() }.minByOrNull { it.date }
    val suggestion = favourites.firstOrNull() ?: recipes.maxByOrNull { it.rating } ?: recipes.firstOrNull()
    val activeTimers = BakeBookTimerScheduler.savedTimers(context, "bake") + BakeBookTimerScheduler.savedTimers(context, "cooling")

    LazyColumn(Modifier.fillMaxSize().background(Cream).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("BakeBook", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Your baking planner, pantry and recipe book.")
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = { showRecipe = true }, modifier = Modifier.weight(1f)) { Text("Add Recipe") }
                Button(onClick = onTimers, modifier = Modifier.weight(1f)) { Text("Add Timer") }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = { quickItem = true }, modifier = Modifier.weight(1f)) { Text("Shopping") }
                Button(onClick = onPhotos, modifier = Modifier.weight(1f)) { Text("Photo") }
            }
        }
        item {
            SummaryCard("Today", "${shopping.count { !it.complete }} shopping items open\n${recipes.size} recipes saved\n${achievements.size} quiet wins unlocked")
        }
        item {
            SummaryCard("Active timers", if (activeTimers.isEmpty()) "No timers running." else activeTimers.joinToString("\n") { it.title })
        }
        item {
            SummaryCard("Suggested bake", suggestion?.let { "${it.title}\n${it.category} - ${it.rating}/5 stars" } ?: "Add a few recipes and BakeBook will suggest one.")
        }
        item { RecipeMiniList("Recently added", recent, onRecipes) }
        item { RecipeMiniList("Favourites", favourites, onRecipes) }
        item {
            SummaryCard("Last baked", lastBaked?.let { "${recipes.firstOrNull { recipe -> recipe.id == it.recipeId }?.title ?: "Recipe"}\n${it.notes.ifBlank { "No notes yet." }}" } ?: "No bake log entries yet.")
        }
        item {
            SummaryCard("Pantry watch", if (expiring.isEmpty()) "No ingredients expiring this week." else expiring.joinToString("\n") { "${it.name}: ${expiryLabel(it.expiryDate)}" })
        }
        item {
            SummaryCard("Next occasion", nextOccasion?.let { "${it.title}\n${DateFormat.getDateInstance().format(it.date)}" } ?: "No occasion planned yet.")
        }
    }

    if (showRecipe) RecipeFormDialog(onDismiss = { showRecipe = false }, onSave = {
        viewModel.saveRecipe(it) { showRecipe = false }
    })
    if (quickItem) QuickTextDialog("Add shopping item", "Item", onDismiss = { quickItem = false }) {
        viewModel.saveShoppingItem(ShoppingItemEntity(name = it))
        quickItem = false
        onShopping()
    }
}

@Composable
fun MoreScreen(
    onLinks: () -> Unit,
    onPhotos: () -> Unit,
    onPantry: () -> Unit,
    onOccasions: () -> Unit,
    onCollections: () -> Unit,
    onSubstitutions: () -> Unit,
    onTools: () -> Unit,
    onConverter: () -> Unit,
    onBackup: () -> Unit
) {
    LazyColumn(Modifier.fillMaxSize().background(Cream).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("More", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Extra BakeBook tools, neatly tucked away.")
        }
        item { MoreActionCard("Saved Links", "Keep online recipe references in one offline list.", onLinks) }
        item { MoreActionCard("Photo Library", "Browse completed bakes and recipe-linked photos.", onPhotos) }
        item { MoreActionCard("Pantry", "Track ingredients, low stock and expiry dates.", onPantry) }
        item { MoreActionCard("Occasions", "Plan batches for birthdays, holidays and events.", onOccasions) }
        item { MoreActionCard("Collections", "Group recipes into sets and seasonal lists.", onCollections) }
        item { MoreActionCard("Substitutions", "Save ingredient swaps and baking notes.", onSubstitutions) }
        item { MoreActionCard("Baking Tools", "Tin, oven, serving and quick reference calculators.", onTools) }
        item { MoreActionCard("Unit Converter", "Convert imperial and metric amounts both ways.", onConverter) }
        item { MoreActionCard("Backup / Restore", "Export and import your offline BakeBook data.", onBackup) }
    }
}

@Composable
private fun MoreActionCard(title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SoftCard),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                Text(title.first().toString(), color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("Open", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SummaryCard(title: String, body: String) {
    Card(colors = CardDefaults.cardColors(containerColor = SoftCard), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RecipeMiniList(title: String, recipes: List<RecipeEntity>, onOpen: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = SoftCard), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().clickable { onOpen() }, elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(if (recipes.isEmpty()) "Nothing here yet." else recipes.joinToString("\n") { "${it.title} - ${it.category}" }, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun PantryScreen(viewModel: BakeBookViewModel) {
    val pantry by viewModel.pantryItems.collectAsState()
    var query by remember { mutableStateOf("") }
    var showForm by remember { mutableStateOf(false) }
    val visible = pantry.filter { it.name.contains(query, true) || it.notes.contains(query, true) }.sortedBy { it.expiryDate ?: Long.MAX_VALUE }
    Scaffold(floatingActionButton = { FloatingActionButton(onClick = { showForm = true }) { Text("+") } }) { padding ->
        LazyColumn(Modifier.fillMaxSize().background(Cream).padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Text("Pantry", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Field("Search pantry", query) { query = it }
            }
            if (visible.isEmpty()) item { EmptyCard("Add pantry items to track stock and expiry dates.") }
            items(visible, key = { it.id }) { item ->
                Card(colors = CardDefaults.cardColors(containerColor = SoftCard), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(item.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            TextButton(onClick = { viewModel.deletePantryItem(item.id) }) { Text("Delete") }
                        }
                        Text("${item.quantity} ${item.unit}".trim())
                        Text("Expiry: ${expiryLabel(item.expiryDate)}")
                        if (item.notes.isNotBlank()) Text(item.notes)
                        if (item.lowStock) Text("Low stock", color = Orange, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
    if (showForm) PantryFormDialog(onDismiss = { showForm = false }) {
        viewModel.savePantryItem(it)
        showForm = false
    }
}

@Composable
private fun PantryFormDialog(onDismiss: () -> Unit, onSave: (PantryItemEntity) -> Unit) {
    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("g") }
    var expiryDays by remember { mutableStateOf("") }
    var lowStock by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add pantry item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Field("Name", name) { name = it }
                Field("Quantity", quantity) { quantity = it }
                MenuButton(unit, listOf("g", "kg", "oz", "lb", "ml", "l", "tsp", "tbsp", "cup")) { unit = it }
                Field("Expiry in days", expiryDays) { expiryDays = it }
                Field("Notes", notes, true) { notes = it }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = lowStock, onCheckedChange = { lowStock = it })
                    Text("Low stock")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val days = expiryDays.toIntOrNull() ?: 0
                val expiry = if (days > 0) System.currentTimeMillis() + days * 24L * 60L * 60L * 1000L else 0L
                onSave(PantryItemEntity(name = name.trim(), quantity = quantity, unit = unit, expiryDate = expiry.takeIf { it > 0L }, lowStock = lowStock, notes = notes))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun CollectionsScreen(viewModel: BakeBookViewModel) {
    val collections by viewModel.collections.collectAsState()
    val links by viewModel.collectionLinks.collectAsState()
    val recipes by viewModel.recipes.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    Scaffold(floatingActionButton = { FloatingActionButton(onClick = { showAdd = true }) { Text("+") } }) { padding ->
        LazyColumn(Modifier.fillMaxSize().background(Cream).padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("Collections", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
            if (collections.isEmpty()) item { EmptyCard("Create collections like Christmas bakes, favourites or weekend ideas.") }
            items(collections, key = { it.id }) { collection ->
                Card(colors = CardDefaults.cardColors(containerColor = SoftCard), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(collection.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            TextButton(onClick = { viewModel.deleteCollection(collection.id) }) { Text("Delete") }
                        }
                        recipes.forEach { recipe ->
                            val checked = links.any { it.collectionId == collection.id && it.recipeId == recipe.id }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = checked, onCheckedChange = {
                                    if (it) viewModel.addRecipeToCollection(recipe.id, collection.id) else viewModel.removeRecipeFromCollection(recipe.id, collection.id)
                                })
                                Text(recipe.title)
                            }
                        }
                    }
                }
            }
        }
    }
    if (showAdd) CollectionDialog(onDismiss = { showAdd = false }) {
        viewModel.saveCollection(it)
        showAdd = false
    }
}

@Composable
private fun CollectionDialog(onDismiss: () -> Unit, onSave: (RecipeCollectionEntity) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add collection") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Field("Name", name) { name = it }
        }
    }, confirmButton = {
        Button(onClick = { onSave(RecipeCollectionEntity(name = name.trim())) }) { Text("Save") }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
fun OccasionPlannerScreen(viewModel: BakeBookViewModel) {
    val occasions by viewModel.occasions.collectAsState()
    val links by viewModel.occasionLinks.collectAsState()
    val recipes by viewModel.recipes.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    Scaffold(floatingActionButton = { FloatingActionButton(onClick = { showAdd = true }) { Text("+") } }) { padding ->
        LazyColumn(Modifier.fillMaxSize().background(Cream).padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("Occasion Planner", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
            if (occasions.isEmpty()) item { EmptyCard("Plan birthdays, bake sales, holidays and family visits.") }
            items(occasions.sortedBy { it.date }, key = { it.id }) { occasion ->
                val selectedRecipeIds = links.filter { it.occasionId == occasion.id }.map { it.recipeId }.toSet()
                Card(colors = CardDefaults.cardColors(containerColor = SoftCard), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(occasion.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            TextButton(onClick = { viewModel.deleteOccasion(occasion.id) }) { Text("Delete") }
                        }
                        Text(DateFormat.getDateInstance().format(occasion.date))
                        Text(occasion.notes.ifBlank { "No notes." })
                        Button(onClick = { selectedRecipeIds.forEach { viewModel.generateShoppingList(it) } }) { Text("Build shopping list") }
                        recipes.forEach { recipe ->
                            val checked = selectedRecipeIds.contains(recipe.id)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = checked, onCheckedChange = {
                                    if (it) viewModel.addRecipeToOccasion(occasion.id, recipe.id) else viewModel.removeRecipeFromOccasion(occasion.id, recipe.id)
                                })
                                Text(recipe.title)
                            }
                        }
                    }
                }
            }
        }
    }
    if (showAdd) OccasionDialog(onDismiss = { showAdd = false }) {
        viewModel.saveOccasion(it)
        showAdd = false
    }
}

@Composable
private fun OccasionDialog(onDismiss: () -> Unit, onSave: (OccasionEntity) -> Unit) {
    var title by remember { mutableStateOf("") }
    var days by remember { mutableStateOf("7") }
    var notes by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add occasion") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Field("Title", title) { title = it }
            Field("Days from now", days) { days = it }
            Field("Notes", notes, true) { notes = it }
        }
    }, confirmButton = {
        Button(onClick = {
            val date = System.currentTimeMillis() + (days.toIntOrNull() ?: 0) * 24L * 60L * 60L * 1000L
            onSave(OccasionEntity(title = title.trim(), date = date, notes = notes))
        }) { Text("Save") }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
fun SubstitutionsScreen(viewModel: BakeBookViewModel) {
    val substitutions by viewModel.substitutions.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    Scaffold(floatingActionButton = { FloatingActionButton(onClick = { showAdd = true }) { Text("+") } }) { padding ->
        LazyColumn(Modifier.fillMaxSize().background(Cream).padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Text("Substitutions", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
            if (substitutions.isEmpty()) item { EmptyCard("Store swaps like buttermilk, egg replacements or gluten-free flour notes.") }
            items(substitutions, key = { it.id }) { item ->
                Card(colors = CardDefaults.cardColors(containerColor = SoftCard), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(item.ingredient, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            TextButton(onClick = { viewModel.deleteSubstitution(item.id) }) { Text("Delete") }
                        }
                        Text(item.substitute)
                        Text(item.notes)
                    }
                }
            }
        }
    }
    if (showAdd) SubstitutionDialog(onDismiss = { showAdd = false }) {
        viewModel.saveSubstitution(it)
        showAdd = false
    }
}

@Composable
private fun SubstitutionDialog(onDismiss: () -> Unit, onSave: (SubstitutionEntity) -> Unit) {
    var ingredient by remember { mutableStateOf("") }
    var replacement by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add substitution") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Field("Ingredient", ingredient) { ingredient = it }
            Field("Replacement", replacement) { replacement = it }
            Field("Notes", notes, true) { notes = it }
        }
    }, confirmButton = {
        Button(onClick = { onSave(SubstitutionEntity(ingredient = ingredient.trim(), substitute = replacement, notes = notes)) }) { Text("Save") }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
fun BakingToolsScreen() {
    var oven by remember { mutableStateOf("180") }
    var servings by remember { mutableStateOf("12") }
    var targetServings by remember { mutableStateOf("24") }
    var roundTin by remember { mutableStateOf("8") }
    val fan = ((oven.toDoubleOrNull() ?: 0.0) - 20.0).coerceAtLeast(0.0)
    val scale = (targetServings.toDoubleOrNull() ?: 0.0) / (servings.toDoubleOrNull() ?: 1.0)
    val squareTin = (roundTin.toDoubleOrNull() ?: 0.0) * 0.89
    LazyColumn(Modifier.fillMaxSize().background(Cream).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Baking Tools", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        item {
            SummaryCard("Oven temperature", "Conventional ${oven.ifBlank { "0" }}C\nFan oven ${formatNumber(fan)}C")
            Field("Conventional oven C", oven) { oven = it }
        }
        item {
            SummaryCard("Recipe scaler", "Scale ingredients by x${formatNumber(scale)}")
            Field("Current servings", servings) { servings = it }
            Spacer(Modifier.height(8.dp))
            Field("Target servings", targetServings) { targetServings = it }
        }
        item {
            SummaryCard("Tin guide", "${roundTin.ifBlank { "0" }} inch round is roughly ${formatNumber(squareTin)} inch square.")
            Field("Round tin inches", roundTin) { roundTin = it }
        }
        item { Section("Quick reference", "1 oz = 28.35 g\n1 lb = 453.59 g\n1 cup = 240 ml\n1 tbsp = 14.79 ml\n1 tsp = 4.93 ml") }
    }
}

@Composable
fun BakeModeScreen(viewModel: BakeBookViewModel, recipeId: Long) {
    var details by remember { mutableStateOf<RecipeWithDetails?>(null) }
    var stepIndex by remember { mutableIntStateOf(0) }
    var checkedIngredients by remember { mutableStateOf(setOf<Long>()) }
    var keepAwake by remember { mutableStateOf(true) }
    var note by remember { mutableStateOf("") }
    val context = LocalContext.current
    val view = LocalView.current
    LaunchedEffect(recipeId) { viewModel.recipeDetails(recipeId) { details = it } }
    DisposableEffect(keepAwake) {
        val window = (view.context as? Activity)?.window
        if (keepAwake) window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
    val current = details
    if (current == null) {
        Box(Modifier.fillMaxSize().background(Cream), contentAlignment = Alignment.Center) { Text("Loading recipe...") }
        return
    }
    val step = current.steps.getOrNull(stepIndex)
    LazyColumn(Modifier.fillMaxSize().background(Cream).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Bake Mode", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(current.recipe.title, style = MaterialTheme.typography.titleLarge)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = keepAwake, onCheckedChange = { keepAwake = it })
                Text("Keep screen awake")
            }
        }
        item {
            SummaryCard("Ingredients checklist", if (current.ingredients.isEmpty()) "No ingredients saved." else "")
            current.ingredients.forEach { ingredient ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = checkedIngredients.contains(ingredient.id), onCheckedChange = {
                        checkedIngredients = if (it) checkedIngredients + ingredient.id else checkedIngredients - ingredient.id
                    })
                    Text(ingredient.text)
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = SoftCard), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Step ${stepIndex + 1} of ${current.steps.size.coerceAtLeast(1)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(step?.text ?: "No method steps saved.")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { stepIndex = (stepIndex - 1).coerceAtLeast(0) }, enabled = stepIndex > 0) { Text("Previous") }
                        Button(onClick = { stepIndex = (stepIndex + 1).coerceAtMost((current.steps.size - 1).coerceAtLeast(0)) }, enabled = stepIndex < current.steps.size - 1) { Text("Next") }
                    }
                }
            }
        }
        item {
            Button(onClick = {
                val minutes = Regex("""\d+""").find(current.recipe.bakeTime)?.value?.toIntOrNull() ?: 30
                BakeBookTimerScheduler.schedule(context, BakeBookTimerScheduler.newTimer("bake", "${current.recipe.title} bake"), minutes * 60_000L)
                toast(context, "Bake timer started.")
            }) { Text("Start recipe bake timer") }
        }
        item {
            Field("Change for next time", note, true) { note = it }
            Button(onClick = {
                if (note.isNotBlank()) {
                    viewModel.updateRecipe(current.recipe.copy(notes = listOf(current.recipe.notes, "Next time: $note").filter { it.isNotBlank() }.joinToString("\n")))
                    note = ""
                    toast(context, "Note added.")
                }
            }) { Text("Save note") }
        }
    }
}

@Composable
fun RecipeScreen(viewModel: BakeBookViewModel, onOpen: (Long) -> Unit) {
    val recipes by viewModel.recipes.collectAsState()
    val collections by viewModel.collections.collectAsState()
    val collectionLinks by viewModel.collectionLinks.collectAsState()
    var search by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("All") }
    var difficulty by remember { mutableStateOf("All") }
    var collection by remember { mutableStateOf("All") }
    var favouriteOnly by remember { mutableStateOf(false) }
    var minRating by remember { mutableStateOf(0) }
    var sort by remember { mutableStateOf(RecipeSort.Date) }
    var showForm by remember { mutableStateOf(false) }
    val selectedCollectionId = collections.firstOrNull { it.name == collection }?.id
    val visible = recipes
        .filter { it.title.contains(search, true) || it.category.contains(search, true) }
        .filter { category == "All" || it.category == category }
        .filter { difficulty == "All" || it.difficulty == difficulty }
        .filter { !favouriteOnly || it.favourite }
        .filter { it.rating >= minRating }
        .filter { selectedCollectionId == null || collectionLinks.any { link -> link.recipeId == it.id && link.collectionId == selectedCollectionId } }
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
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    MenuButton(label = difficulty, options = listOf("All") + RecipeDifficulty.entries.map { it.name }, onSelected = { difficulty = it })
                    MenuButton(label = collection, options = listOf("All") + collections.map { it.name }, onSelected = { collection = it })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(selected = favouriteOnly, onClick = { favouriteOnly = !favouriteOnly }, label = { Text("Favourites") })
                    Text("Min rating: $minRating")
                    Slider(value = minRating.toFloat(), onValueChange = { minRating = it.toInt() }, valueRange = 0f..5f, steps = 4, modifier = Modifier.weight(1f))
                }
            }
            if (visible.isEmpty()) item { EmptyCard("No recipes yet. Add your first bake.") }
            items(visible, key = { it.id }) { recipe ->
                PremiumRecipeCard(recipe, onOpen, onFavourite = { viewModel.toggleFavourite(recipe) }, onDuplicate = { viewModel.duplicateRecipe(recipe.id) }, onDelete = { viewModel.deleteRecipe(recipe.id) })
            }
        }
    }
    if (showForm) RecipeFormDialog(collections = collections, onDismiss = { showForm = false }, onSave = {
        viewModel.saveRecipe(it) { message -> showForm = false }
    })
}

@Composable
private fun SearchAndFilters(search: String, onSearch: (String) -> Unit, category: String, onCategory: (String) -> Unit, sort: RecipeSort, onSort: (RecipeSort) -> Unit) {
    OutlinedTextField(value = search, onValueChange = onSearch, label = { Text("Search recipes") }, placeholder = { Text("Cake, brownies, bread...") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(18.dp))
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        MenuButton(label = category, options = listOf("All") + RecipeCategory.entries.map { it.name }, onSelected = onCategory)
        MenuButton(label = sort.name, options = RecipeSort.entries.map { it.name }, onSelected = { onSort(RecipeSort.valueOf(it)) })
    }
}

@Composable
private fun PremiumRecipeCard(recipe: RecipeEntity, onOpen: (Long) -> Unit, onFavourite: () -> Unit, onDuplicate: () -> Unit, onDelete: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = SoftCard), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth().clickable { onOpen(recipe.id) }, elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (recipe.coverPhotoUri.isNotBlank()) {
                Image(
                    painter = rememberAsyncImagePainter(recipe.coverPhotoUri),
                    contentDescription = recipe.title,
                    modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(recipe.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    TextButton(onClick = onFavourite) { Text(if (recipe.favourite) "Favourite" else "Save") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    InfoChip(recipe.category)
                    InfoChip(recipe.difficulty)
                    InfoChip("${recipe.rating}/5")
                }
                Text("Prep ${recipe.prepTime.ifBlank { "-" }}   Bake ${recipe.bakeTime.ifBlank { "-" }}   ${recipe.ovenTemperature.ifBlank { "-" }}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Serves ${recipe.servings.ifBlank { "-" }}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = onDuplicate, label = { Text("Duplicate") })
                    AssistChip(onClick = onDelete, label = { Text("Delete") })
                }
            }
        }
    }
}

@Composable
private fun InfoChip(text: String) {
    Box(Modifier.clip(RoundedCornerShape(999.dp)).background(MaterialTheme.colorScheme.primaryContainer).padding(horizontal = 10.dp, vertical = 6.dp)) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
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
            Text("${recipe.difficulty} • Serves ${recipe.servings.ifBlank { "-" }} • ${"★".repeat(recipe.rating.coerceIn(0, 5))}${"☆".repeat(5 - recipe.rating.coerceIn(0, 5))}")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = onDuplicate, label = { Text("Duplicate") })
                AssistChip(onClick = onDelete, label = { Text("Delete") })
            }
        }
    }
}

@Composable
private fun RecipeFormDialog(collections: List<RecipeCollectionEntity> = emptyList(), onDismiss: () -> Unit, onSave: (RecipeForm) -> Unit) {
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
                item { MenuButton(form.difficulty, RecipeDifficulty.entries.map { it.name }) { form = form.copy(difficulty = it) } }
                item { Field("Prep Time", form.prepTime) { form = form.copy(prepTime = it) } }
                item { Field("Bake Time", form.bakeTime) { form = form.copy(bakeTime = it) } }
                item { Field("Oven Temperature", form.ovenTemperature) { form = form.copy(ovenTemperature = it) } }
                item { Field("Servings", form.servings) { form = form.copy(servings = it) } }
                item {
                    DynamicTextRows(
                        title = "Ingredients (oz)",
                        addLabel = "Add ingredient",
                        itemLabel = { index -> "Ingredient ${index + 1} - optional cost: | 1.20" },
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
fun RecipeDetailScreen(viewModel: BakeBookViewModel, recipeId: Long, onBakeMode: (Long) -> Unit = {}) {
    val context = LocalContext.current
    val collections by viewModel.collections.collectAsState()
    val collectionLinks by viewModel.collectionLinks.collectAsState()
    val bakeLogs by viewModel.bakeLogs.collectAsState()
    val photos by viewModel.photos.collectAsState()
    val substitutions by viewModel.substitutions.collectAsState()
    var details by remember { mutableStateOf<RecipeWithDetails?>(null) }
    var scale by remember { mutableFloatStateOf(1f) }
    var showBakeLog by remember { mutableStateOf(false) }
    LaunchedEffect(recipeId) { viewModel.recipeDetails(recipeId) { details = it } }
    val current = details
    if (current == null) {
        EmptyCard("Recipe not found.")
        return
    }
    LazyColumn(Modifier.fillMaxSize().background(Cream).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = SoftCard), shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (current.recipe.coverPhotoUri.isNotBlank()) {
                        Image(
                            painter = rememberAsyncImagePainter(current.recipe.coverPhotoUri),
                            contentDescription = current.recipe.title,
                            modifier = Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(current.recipe.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            InfoChip(current.recipe.category)
                            InfoChip(current.recipe.difficulty)
                            InfoChip("${current.recipe.rating}/5")
                        }
                        Text("Prep ${current.recipe.prepTime.ifBlank { "-" }}   Bake ${current.recipe.bakeTime.ifBlank { "-" }}   Oven ${current.recipe.ovenTemperature.ifBlank { "-" }}   Serves ${current.recipe.servings.ifBlank { "-" }}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(onClick = { onBakeMode(recipeId) }, modifier = Modifier.weight(1f)) { Text("Bake Mode") }
                            Button(onClick = { viewModel.generateShoppingList(recipeId); toast(context, "Shopping list generated.") }, modifier = Modifier.weight(1f)) { Text("Shopping") }
                        }
                    }
                }
            }
        }
        item {
            Text(current.recipe.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("${current.recipe.category} • ${current.recipe.difficulty} • Rating ${current.recipe.rating}/5 • Favourite ${if (current.recipe.favourite) "Yes" else "No"}")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                Button(onClick = { shareText(context, recipeAsText(current, scale)) }) { Text("Share") }
                Button(onClick = { exportPdf(context, current, scale) }) { Text("Export PDF") }
                Button(onClick = { viewModel.generateShoppingList(recipeId); toast(context, "Shopping list generated.") }) { Text("Shopping List") }
                Button(onClick = { onBakeMode(recipeId) }) { Text("Bake Mode") }
            }
        }
        item { ScaleControls(scale) { scale = it } }
        item { Section("Ingredients (oz)", current.ingredients.joinToString("\n") { scaleIngredient(it.text, scale) }) }
        if (current.ingredients.any { it.cost > 0.0 }) {
            item {
                val total = current.ingredients.sumOf { it.cost }
                val servings = current.recipe.servings.toDoubleOrNull()?.takeIf { it > 0 } ?: 1.0
                Section("Cost Estimate", "Batch: £${"%.2f".format(total)}\nPer serving: £${"%.2f".format(total / servings)}")
            }
        }
        item { Section("Method", current.steps.mapIndexed { i, step -> "${i + 1}. ${step.text}" }.joinToString("\n")) }
        item { Section("Notes", current.recipe.notes.ifBlank { "No notes." }) }
        item {
            val recipeCollections = collectionLinks.filter { it.recipeId == recipeId }.mapNotNull { link -> collections.firstOrNull { it.id == link.collectionId }?.name }
            Section("Collections", recipeCollections.ifEmpty { listOf("No collections yet.") }.joinToString("\n"))
        }
        item {
            val relevantSubs = substitutions.filter { sub ->
                sub.recipeId == recipeId || current.ingredients.any { it.text.contains(sub.ingredient, true) }
            }
            Section("Substitutions", relevantSubs.ifEmpty { listOf(SubstitutionEntity(ingredient = "", substitute = "No substitutions saved yet.")) }.joinToString("\n") { sub ->
                if (sub.ingredient.isBlank()) sub.substitute else "${sub.ingredient} -> ${sub.substitute}${if (sub.notes.isBlank()) "" else " (${sub.notes})"}"
            })
        }
        item {
            RecipePhotoGallery(photos.filter { it.linkedRecipeId == recipeId }, onSetCover = { photo ->
                viewModel.updateRecipe(current.recipe.copy(coverPhotoUri = photo.uri))
                details = current.copy(recipe = current.recipe.copy(coverPhotoUri = photo.uri))
            }, onDelete = { viewModel.deletePhoto(it.id) })
        }
        item {
            BakeHistorySection(
                logs = bakeLogs.filter { it.recipeId == recipeId },
                onAdd = { showBakeLog = true },
                onDelete = { viewModel.deleteBakeLog(it.id) }
            )
        }
    }
    if (showBakeLog) BakeLogDialog(recipeId = recipeId, onDismiss = { showBakeLog = false }) {
        viewModel.saveBakeLog(it)
        showBakeLog = false
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
private fun RecipePhotoGallery(photos: List<PhotoEntryEntity>, onSetCover: (PhotoEntryEntity) -> Unit, onDelete: (PhotoEntryEntity) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = SoftCard), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Recipe Photos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (photos.isEmpty()) {
                Text("No photos linked to this recipe yet.")
            } else {
                photos.forEach { photo ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Image(painter = rememberAsyncImagePainter(photo.uri), contentDescription = photo.title, modifier = Modifier.size(72.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                        Column(Modifier.weight(1f)) {
                            Text(photo.title, fontWeight = FontWeight.Bold)
                            Text(photo.caption.ifBlank { photo.notes.ifBlank { "No caption." } })
                        }
                        TextButton(onClick = { onSetCover(photo) }) { Text("Cover") }
                        TextButton(onClick = { onDelete(photo) }) { Text("Delete") }
                    }
                }
            }
        }
    }
}

@Composable
private fun BakeHistorySection(logs: List<BakeLogEntity>, onAdd: () -> Unit, onDelete: (BakeLogEntity) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = SoftCard), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Bake History", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Button(onClick = onAdd) { Text("Add") }
            }
            if (logs.isEmpty()) {
                Text("No bakes recorded yet.")
            } else {
                logs.forEach { log ->
                    Card(colors = CardDefaults.cardColors(containerColor = Cream), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("${DateFormat.getDateInstance().format(log.dateBaked)} • Result ${log.resultRating}/5")
                            Text(log.notes.ifBlank { "No notes." })
                            TextButton(onClick = { onDelete(log) }) { Text("Delete") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BakeLogDialog(recipeId: Long, onDismiss: () -> Unit, onSave: (BakeLogEntity) -> Unit) {
    var notes by remember { mutableStateOf("") }
    var changes by remember { mutableStateOf("") }
    var bakeTime by remember { mutableStateOf("") }
    var oven by remember { mutableStateOf("") }
    var rating by remember { mutableIntStateOf(0) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Bake Log") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Field("Notes", notes, true) { notes = it }
                Field("Changes made", changes, true) { changes = it }
                Field("Actual bake time", bakeTime) { bakeTime = it }
                Field("Actual oven temperature", oven) { oven = it }
                Text("Result: $rating/5")
                Slider(value = rating.toFloat(), onValueChange = { rating = it.toInt() }, valueRange = 0f..5f, steps = 4)
            }
        },
        confirmButton = { Button(onClick = { onSave(BakeLogEntity(recipeId = recipeId, notes = notes, changesMade = changes, actualBakeTime = bakeTime, actualOvenTemperature = oven, resultRating = rating)) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
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
    var caption by remember { mutableStateOf("") }
    var uri by remember { mutableStateOf("") }
    var linkedId by remember { mutableStateOf<Long?>(null) }
    var isCover by remember { mutableStateOf(false) }
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
                Field("Caption", caption) { caption = it }
                Field("Notes", notes, true) { notes = it }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isCover, onCheckedChange = { isCover = it })
                    Text("Use as recipe cover")
                }
            }
        },
        confirmButton = { Button(onClick = { onSave(PhotoEntryEntity(title = title, uri = uri, linkedRecipeId = linkedId, notes = notes, caption = caption, isCover = isCover)) }) { Text("Save") } },
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
    val pendingItems = items.filterNot { it.complete }
    val completedItems = items.filter { it.complete }
    LazyColumn(Modifier.fillMaxSize().background(Cream).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Shopping List", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Item") }, placeholder = { Text("Caster sugar, butter...") }, modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(16.dp))
                Spacer(Modifier.width(8.dp))
                Button(onClick = { if (name.isNotBlank()) { viewModel.saveShoppingItem(ShoppingItemEntity(name = name.trim())); name = "" } }) { Text("Add") }
            }
            TextButton(onClick = { viewModel.clearCompletedShopping() }) { Text("Clear completed") }
        }
        item { Text("To buy", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        if (pendingItems.isEmpty()) item { EmptyCard("Your shopping list is clear.") }
        items(pendingItems, key = { it.id }) { item ->
            ShoppingItemRow(item = item, softened = false, onToggle = { viewModel.saveShoppingItem(item.copy(complete = it)) }, onDelete = { viewModel.deleteShoppingItem(item.id) })
        }
        if (completedItems.isNotEmpty()) {
            item { Text("Completed", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            items(completedItems, key = { it.id }) { item ->
                ShoppingItemRow(item = item, softened = true, onToggle = { viewModel.saveShoppingItem(item.copy(complete = it)) }, onDelete = { viewModel.deleteShoppingItem(item.id) })
            }
        }
    }
}

@Composable
private fun ShoppingItemRow(item: ShoppingItemEntity, softened: Boolean, onToggle: (Boolean) -> Unit, onDelete: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = if (softened) MaterialTheme.colorScheme.surfaceVariant else SoftCard), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = if (softened) 0.dp else 1.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = item.complete, onCheckedChange = onToggle)
            Column(Modifier.weight(1f)) {
                Text(item.name, color = if (softened) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                if (item.status.isNotBlank()) Text(item.status, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
            TextButton(onClick = onDelete) { Text("Delete") }
        }
    }
}

@Composable
fun TimerScreen() {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    LazyColumn(Modifier.fillMaxSize().background(Cream).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Baking Timers", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Timers keep running when BakeBook is minimised or the phone is locked.")
        }
        item {
            TimerSection(
                type = "bake",
                title = "Bake Countdown",
                defaultMinutes = 30,
                presets = listOf(10, 15, 20, 30, 45, 60),
                now = now
            )
        }
        item {
            TimerSection(
                type = "cooling",
                title = "Cooling Clock",
                defaultMinutes = 20,
                presets = listOf(5, 10, 15, 20, 30, 45),
                now = now
            )
        }
    }
}

@Composable
private fun TimerSection(type: String, title: String, defaultMinutes: Int, presets: List<Int>, now: Long) {
    val context = LocalContext.current
    var timers by remember(type) {
        val saved = BakeBookTimerScheduler.savedTimers(context, type)
        mutableStateOf(saved.ifEmpty { listOf(BakeBookTimerScheduler.newTimer(type, title)) })
    }
    Card(colors = CardDefaults.cardColors(containerColor = SoftCard), shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Button(onClick = {
                    timers = timers + BakeBookTimerScheduler.newTimer(type, "$title ${timers.size + 1}")
                }) { Text("Add") }
            }
            timers.forEach { timer ->
                CountdownClock(
                    timer = timer,
                    defaultMinutes = defaultMinutes,
                    presets = presets,
                    now = now,
                    onRemove = {
                        timers = timers.filterNot { it.id == timer.id }
                            .ifEmpty { listOf(BakeBookTimerScheduler.newTimer(type, title)) }
                    }
                )
            }
        }
    }
}

@Composable
private fun CountdownClock(timer: BakeTimerDefinition, defaultMinutes: Int, presets: List<Int>, now: Long, onRemove: () -> Unit) {
    val context = LocalContext.current
    var customMinutes by remember { mutableStateOf(defaultMinutes.toString()) }
    var endAt by remember { mutableLongStateOf(BakeBookTimerScheduler.endAt(context, timer)) }
    var durationMillis by remember { mutableLongStateOf(BakeBookTimerScheduler.duration(context, timer)) }
    val remainingMillis = (endAt - now).coerceAtLeast(0L)
    val active = remainingMillis > 0L
    val progress = if (durationMillis > 0L && active) remainingMillis.toFloat() / durationMillis else 0f

    LaunchedEffect(now) {
        if (endAt > 0L && remainingMillis == 0L) {
            endAt = 0L
            durationMillis = 0L
        }
    }

    Card(colors = CardDefaults.cardColors(containerColor = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = if (active) 1.dp else 0.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(timer.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(formatTimer(remainingMillis), style = MaterialTheme.typography.displayMedium, color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = customMinutes,
                    onValueChange = { customMinutes = it.filter(Char::isDigit) },
                    label = { Text("Minutes") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Button(onClick = {
                    val minutes = customMinutes.toLongOrNull()?.coerceIn(1, 999) ?: defaultMinutes.toLong()
                    durationMillis = minutes * 60_000L
                    endAt = BakeBookTimerScheduler.schedule(context, timer, durationMillis)
                }) { Text(if (active) "Restart" else "Start") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                presets.forEach { minutes ->
                    AssistChip(onClick = {
                        customMinutes = minutes.toString()
                        durationMillis = minutes * 60_000L
                        endAt = BakeBookTimerScheduler.schedule(context, timer, durationMillis)
                    }, label = { Text("${minutes}m") })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(enabled = active, onClick = {
                    customMinutes = ((remainingMillis + 59_999L) / 60_000L).coerceAtLeast(1L).toString()
                    BakeBookTimerScheduler.cancel(context, timer)
                    endAt = 0L
                    durationMillis = 0L
                }) { Text("Pause") }
                Button(onClick = {
                    BakeBookTimerScheduler.cancel(context, timer)
                    endAt = 0L
                    durationMillis = 0L
                    onRemove()
                }) { Text("Reset") }
            }
        }
    }
}

@Composable
fun ConverterScreen() {
    var category by remember { mutableStateOf("Mass") }
    var amount by remember { mutableStateOf("") }
    var fromUnit by remember { mutableStateOf(converterUnits.first { it.name == "Pounds" }) }
    var toUnit by remember { mutableStateOf(converterUnits.first { it.name == "Kilogrammes" }) }
    val categoryUnits = converterUnits.filter { it.category == category }
    val converted = convertUnit(amount.toDoubleOrNull() ?: 0.0, fromUnit, toUnit)

    LaunchedEffect(category) {
        if (category == "Mass") {
            fromUnit = converterUnits.first { it.name == "Pounds" }
            toUnit = converterUnits.first { it.name == "Kilogrammes" }
        } else {
            fromUnit = converterUnits.first { it.name == "Fluid ounces" }
            toUnit = converterUnits.first { it.name == "Millilitres" }
        }
        amount = ""
    }

    LazyColumn(
        Modifier.fillMaxSize().background(Cream).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Unit Converter", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Mass", "Volume").forEach { option ->
                    FilterChip(selected = category == option, onClick = { category = option }, label = { Text(option) })
                }
            }
        }
        item {
            ConverterValueRow(
                selectedUnit = fromUnit,
                units = categoryUnits,
                amount = amount,
                onUnitSelected = { fromUnit = it },
                isInput = true
            )
        }
        item {
            Button(
                onClick = {
                    val previous = fromUnit
                    fromUnit = toUnit
                    toUnit = previous
                    amount = formatNumber(converted)
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Swap units") }
        }
        item {
            ConverterValueRow(
                selectedUnit = toUnit,
                units = categoryUnits,
                amount = if (amount.isBlank()) "" else formatNumber(converted),
                onUnitSelected = { toUnit = it },
                isInput = false
            )
        }
        item {
            ConverterKeypad(
                onKey = { key ->
                    amount = when (key) {
                        "C" -> ""
                        "Del" -> amount.dropLast(1)
                        "." -> if (amount.contains(".")) amount else amount.ifBlank { "0" } + "."
                        else -> if (amount == "0") key else amount + key
                    }
                }
            )
        }
    }
}

@Composable
private fun ConverterValueRow(
    selectedUnit: ConverterUnit,
    units: List<ConverterUnit>,
    amount: String,
    onUnitSelected: (ConverterUnit) -> Unit,
    isInput: Boolean
) {
    Card(colors = CardDefaults.cardColors(containerColor = SoftCard), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ConverterUnitMenu(selectedUnit, units, onUnitSelected)
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = amount.ifBlank { if (isInput) "0" else "-" },
                    style = MaterialTheme.typography.displaySmall,
                    modifier = Modifier.weight(1f)
                )
                Text(selectedUnit.symbol, style = MaterialTheme.typography.headlineSmall, color = Orange)
            }
        }
    }
}

@Composable
private fun ConverterUnitMenu(selectedUnit: ConverterUnit, units: List<ConverterUnit>, onUnitSelected: (ConverterUnit) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        ElevatedButton(onClick = { expanded = true }) { Text(selectedUnit.name) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            units.forEach { unit ->
                DropdownMenuItem(text = { Text("${unit.name} (${unit.symbol})") }, onClick = {
                    expanded = false
                    onUnitSelected(unit)
                })
            }
        }
    }
}

@Composable
private fun ConverterKeypad(onKey: (String) -> Unit) {
    val rows = listOf(
        listOf("7", "8", "9", "C"),
        listOf("4", "5", "6", "Del"),
        listOf("1", "2", "3", "."),
        listOf("0")
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { key ->
                    Button(
                        onClick = { onKey(key) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(64.dp)
                    ) { Text(key, style = MaterialTheme.typography.titleLarge) }
                }
                repeat(4 - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun BackupScreen(viewModel: BakeBookViewModel) {
    val recipes by viewModel.recipes.collectAsState()
    val links by viewModel.links.collectAsState()
    val photos by viewModel.photos.collectAsState()
    val shopping by viewModel.shoppingItems.collectAsState()
    val collections by viewModel.collections.collectAsState()
    val collectionLinks by viewModel.collectionLinks.collectAsState()
    val bakeLogs by viewModel.bakeLogs.collectAsState()
    val pantry by viewModel.pantryItems.collectAsState()
    val occasions by viewModel.occasions.collectAsState()
    val occasionLinks by viewModel.occasionLinks.collectAsState()
    val substitutions by viewModel.substitutions.collectAsState()
    val achievements by viewModel.achievements.collectAsState()
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
                Button(onClick = {
                    json = buildBackupJson(recipes, links, photos, shopping, collections, collectionLinks, bakeLogs, pantry, occasions, occasionLinks, substitutions, achievements)
                    shareText(context, json)
                }) { Text("Backup Data") }
                Button(onClick = { restore.launch("application/json") }) { Text("Restore Data") }
            }
            Field("JSON Backup", json, true) { json = it }
            Button(onClick = { restoreBackup(json, viewModel, context) }) { Text("Import JSON") }
        }
    }
}

@Composable
private fun QuickTextDialog(title: String, label: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var value by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Field(label, value) { value = it } },
        confirmButton = {
            Button(onClick = { if (value.isNotBlank()) onSave(value.trim()) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun EmptyCard(text: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Nothing here yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Section(title: String, body: String) {
    Card(colors = CardDefaults.cardColors(containerColor = SoftCard), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        singleLine = !multi,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun MenuButton(label: String, options: List<String>, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        ElevatedButton(onClick = { expanded = true }, shape = RoundedCornerShape(16.dp)) { Text(label) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach {
                DropdownMenuItem(text = { Text(it) }, onClick = { expanded = false; onSelected(it) })
            }
        }
    }
}

private fun expiryLabel(expiryDate: Long?): String {
    val value = expiryDate ?: return "Not set"
    if (value <= 0L) return "Not set"
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val days = ((value - today) / (24L * 60L * 60L * 1000L)).toInt()
    return when {
        days < 0 -> "Expired ${DateFormat.getDateInstance().format(value)}"
        days == 0 -> "Today"
        days == 1 -> "Tomorrow"
        else -> "${DateFormat.getDateInstance().format(value)} ($days days)"
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

private fun buildBackupJson(
    recipes: List<RecipeEntity>,
    links: List<RecipeLinkEntity>,
    photos: List<PhotoEntryEntity>,
    shopping: List<ShoppingItemEntity>,
    collections: List<RecipeCollectionEntity>,
    collectionLinks: List<RecipeCollectionLinkEntity>,
    bakeLogs: List<BakeLogEntity>,
    pantry: List<PantryItemEntity>,
    occasions: List<OccasionEntity>,
    occasionLinks: List<OccasionRecipeLinkEntity>,
    substitutions: List<SubstitutionEntity>,
    achievements: List<AchievementEntity>
): String {
    val root = JSONObject()
    root.put("recipes", JSONArray(recipes.map {
        JSONObject()
            .put("id", it.id)
            .put("title", it.title)
            .put("category", it.category)
            .put("prepTime", it.prepTime)
            .put("bakeTime", it.bakeTime)
            .put("ovenTemperature", it.ovenTemperature)
            .put("servings", it.servings)
            .put("notes", it.notes)
            .put("rating", it.rating)
            .put("difficulty", it.difficulty)
            .put("coverPhotoUri", it.coverPhotoUri)
            .put("favourite", it.favourite)
    }))
    root.put("links", JSONArray(links.map { JSONObject().put("title", it.title).put("websiteName", it.websiteName).put("url", it.url).put("notes", it.notes).put("category", it.category) }))
    root.put("photos", JSONArray(photos.map { JSONObject().put("title", it.title).put("uri", it.uri).put("linkedRecipeId", it.linkedRecipeId).put("notes", it.notes).put("caption", it.caption).put("isCover", it.isCover).put("dateBaked", it.dateBaked) }))
    root.put("shoppingItems", JSONArray(shopping.map { JSONObject().put("name", it.name).put("complete", it.complete).put("status", it.status).put("sourceRecipeId", it.sourceRecipeId) }))
    root.put("collections", JSONArray(collections.map { JSONObject().put("id", it.id).put("name", it.name).put("dateCreated", it.dateCreated) }))
    root.put("collectionLinks", JSONArray(collectionLinks.map { JSONObject().put("recipeId", it.recipeId).put("collectionId", it.collectionId) }))
    root.put("bakeLogs", JSONArray(bakeLogs.map {
        JSONObject()
            .put("recipeId", it.recipeId)
            .put("dateBaked", it.dateBaked)
            .put("notes", it.notes)
            .put("resultRating", it.resultRating)
            .put("actualBakeTime", it.actualBakeTime)
            .put("actualOvenTemperature", it.actualOvenTemperature)
            .put("changesMade", it.changesMade)
            .put("linkedPhotoIds", it.linkedPhotoIds)
    }))
    root.put("pantryItems", JSONArray(pantry.map { JSONObject().put("name", it.name).put("quantity", it.quantity).put("unit", it.unit).put("expiryDate", it.expiryDate).put("lowStock", it.lowStock).put("minimumQuantity", it.minimumQuantity).put("notes", it.notes) }))
    root.put("occasions", JSONArray(occasions.map { JSONObject().put("id", it.id).put("title", it.title).put("date", it.date).put("notes", it.notes).put("bakeSchedule", it.bakeSchedule).put("completed", it.completed) }))
    root.put("occasionLinks", JSONArray(occasionLinks.map { JSONObject().put("occasionId", it.occasionId).put("recipeId", it.recipeId) }))
    root.put("substitutions", JSONArray(substitutions.map { JSONObject().put("ingredient", it.ingredient).put("substitute", it.substitute).put("notes", it.notes).put("recipeId", it.recipeId) }))
    root.put("achievements", JSONArray(achievements.map { JSONObject().put("key", it.key).put("title", it.title).put("unlockedAt", it.unlockedAt) }))
    return root.toString(2)
}

private data class ConverterUnit(
    val category: String,
    val name: String,
    val symbol: String,
    val factorToBase: Double
)

private val converterUnits = listOf(
    ConverterUnit("Mass", "Kilogrammes", "kg", 1000.0),
    ConverterUnit("Mass", "Grammes", "g", 1.0),
    ConverterUnit("Mass", "Pounds", "lb", 453.59237),
    ConverterUnit("Mass", "Ounces", "oz", 28.349523125),
    ConverterUnit("Volume", "Litres", "l", 1000.0),
    ConverterUnit("Volume", "Millilitres", "ml", 1.0),
    ConverterUnit("Volume", "Fluid ounces", "fl oz", 29.5735295625),
    ConverterUnit("Volume", "Cups", "cup", 240.0),
    ConverterUnit("Volume", "Tablespoons", "tbsp", 14.78676478125),
    ConverterUnit("Volume", "Teaspoons", "tsp", 4.92892159375)
)

private fun convertUnit(amount: Double, fromUnit: ConverterUnit, toUnit: ConverterUnit): Double {
    if (amount <= 0.0) return 0.0
    return amount * fromUnit.factorToBase / toUnit.factorToBase
}

private fun formatNumber(value: Double): String {
    if (value == 0.0) return "0"
    return "%.4f".format(value).trimEnd('0').trimEnd('.')
}

private fun formatTimer(remainingMillis: Long): String {
    val totalSeconds = (remainingMillis / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        "%02d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

private fun restoreBackup(json: String, viewModel: BakeBookViewModel, context: Context) {
    runCatching {
        val root = JSONObject(json)
        root.optJSONArray("recipes")?.let { array ->
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                viewModel.saveRecipe(
                    RecipeForm(
                        title = item.optString("title"),
                        category = item.optString("category", RecipeCategory.Other.name),
                        prepTime = item.optString("prepTime"),
                        bakeTime = item.optString("bakeTime"),
                        ovenTemperature = item.optString("ovenTemperature"),
                        servings = item.optString("servings"),
                        notes = item.optString("notes"),
                        rating = item.optInt("rating"),
                        difficulty = item.optString("difficulty", RecipeDifficulty.Easy.name),
                        favourite = item.optBoolean("favourite")
                    )
                )
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
                viewModel.savePhoto(PhotoEntryEntity(title = item.optString("title"), uri = item.optString("uri"), linkedRecipeId = item.optLong("linkedRecipeId").takeIf { it > 0L }, notes = item.optString("notes"), caption = item.optString("caption"), isCover = item.optBoolean("isCover"), dateBaked = item.optLong("dateBaked", System.currentTimeMillis())))
            }
        }
        root.optJSONArray("shoppingItems")?.let { array ->
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                viewModel.saveShoppingItem(ShoppingItemEntity(name = item.optString("name"), complete = item.optBoolean("complete"), status = item.optString("status"), sourceRecipeId = item.optLong("sourceRecipeId").takeIf { it > 0L }))
            }
        }
        root.optJSONArray("collections")?.let { array ->
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                viewModel.saveCollection(RecipeCollectionEntity(name = item.optString("name"), dateCreated = item.optLong("dateCreated", System.currentTimeMillis())))
            }
        }
        root.optJSONArray("collectionLinks")?.let { array ->
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                viewModel.addRecipeToCollection(item.optLong("recipeId"), item.optLong("collectionId"))
            }
        }
        root.optJSONArray("bakeLogs")?.let { array ->
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                viewModel.saveBakeLog(
                    BakeLogEntity(
                        recipeId = item.optLong("recipeId"),
                        dateBaked = item.optLong("dateBaked", System.currentTimeMillis()),
                        notes = item.optString("notes"),
                        resultRating = item.optInt("resultRating"),
                        actualBakeTime = item.optString("actualBakeTime"),
                        actualOvenTemperature = item.optString("actualOvenTemperature"),
                        changesMade = item.optString("changesMade"),
                        linkedPhotoIds = item.optString("linkedPhotoIds")
                    )
                )
            }
        }
        root.optJSONArray("pantryItems")?.let { array ->
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                viewModel.savePantryItem(PantryItemEntity(name = item.optString("name"), quantity = item.optString("quantity"), unit = item.optString("unit"), expiryDate = item.optLong("expiryDate").takeIf { it > 0L }, lowStock = item.optBoolean("lowStock"), minimumQuantity = item.optString("minimumQuantity"), notes = item.optString("notes")))
            }
        }
        root.optJSONArray("occasions")?.let { array ->
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                viewModel.saveOccasion(OccasionEntity(title = item.optString("title"), date = item.optLong("date", System.currentTimeMillis()), notes = item.optString("notes"), bakeSchedule = item.optString("bakeSchedule"), completed = item.optBoolean("completed")))
            }
        }
        root.optJSONArray("occasionLinks")?.let { array ->
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                viewModel.addRecipeToOccasion(item.optLong("occasionId"), item.optLong("recipeId"))
            }
        }
        root.optJSONArray("substitutions")?.let { array ->
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                viewModel.saveSubstitution(SubstitutionEntity(ingredient = item.optString("ingredient"), substitute = item.optString("substitute"), notes = item.optString("notes"), recipeId = item.optLong("recipeId").takeIf { it > 0L }))
            }
        }
        root.optJSONArray("achievements")?.let { array ->
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                viewModel.unlockAchievement(AchievementEntity(key = item.optString("key"), title = item.optString("title"), unlockedAt = item.optLong("unlockedAt", System.currentTimeMillis())))
            }
        }
    }.onSuccess { toast(context, "Backup restored.") }
        .onFailure { toast(context, "Could not restore backup: ${it.message}") }
}

private fun toast(context: Context, message: String) = Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
