package com.robbiebedford.bakebook.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.robbiebedford.bakebook.data.database.PhotoEntryEntity
import com.robbiebedford.bakebook.data.database.RecipeCategory
import com.robbiebedford.bakebook.data.database.RecipeEntity
import com.robbiebedford.bakebook.data.database.RecipeLinkEntity
import com.robbiebedford.bakebook.data.database.RecipeWithDetails
import com.robbiebedford.bakebook.data.database.ShoppingItemEntity
import com.robbiebedford.bakebook.data.repository.BakeBookRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class RecipeSort { Name, Date, Rating, Category }

data class RecipeForm(
    val id: Long = 0,
    val title: String = "",
    val category: String = RecipeCategory.Cakes.name,
    val prepTime: String = "",
    val bakeTime: String = "",
    val ovenTemperature: String = "",
    val servings: String = "",
    val ingredients: String = "",
    val method: String = "",
    val notes: String = "",
    val rating: Int = 0,
    val favourite: Boolean = false
)

class BakeBookViewModel(private val repository: BakeBookRepository) : ViewModel() {
    val recipes: StateFlow<List<RecipeEntity>> = repository.recipes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val links: StateFlow<List<RecipeLinkEntity>> = repository.links.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val photos: StateFlow<List<PhotoEntryEntity>> = repository.photos.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val shoppingItems: StateFlow<List<ShoppingItemEntity>> = repository.shoppingItems.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val backupJson: StateFlow<String> = repository.recipes.map { "BakeBook backup contains ${it.size} recipes." }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    fun saveRecipe(form: RecipeForm, onDone: (String) -> Unit = {}) = viewModelScope.launch {
        runCatching {
            require(form.title.isNotBlank()) { "Recipe title is required." }
            repository.saveRecipe(
                RecipeEntity(
                    id = form.id,
                    title = form.title.trim(),
                    category = form.category,
                    prepTime = form.prepTime,
                    bakeTime = form.bakeTime,
                    ovenTemperature = form.ovenTemperature,
                    servings = form.servings,
                    notes = form.notes,
                    rating = form.rating.coerceIn(0, 5),
                    favourite = form.favourite
                ),
                form.ingredients.lines(),
                form.method.lines()
            )
        }.onSuccess { onDone("Recipe saved.") }
            .onFailure { onDone(it.message ?: "Could not save recipe.") }
    }

    fun toggleFavourite(recipe: RecipeEntity) = viewModelScope.launch {
        repository.updateRecipe(recipe.copy(favourite = !recipe.favourite))
    }

    fun deleteRecipe(id: Long) = viewModelScope.launch { repository.deleteRecipe(id) }
    fun duplicateRecipe(id: Long) = viewModelScope.launch { repository.duplicateRecipe(id) }
    fun generateShoppingList(id: Long) = viewModelScope.launch { repository.generateShoppingList(id) }
    fun recipeDetails(id: Long, onResult: (RecipeWithDetails?) -> Unit) = viewModelScope.launch { onResult(repository.recipeDetails(id)) }

    fun saveLink(link: RecipeLinkEntity, onDone: (String) -> Unit = {}) = viewModelScope.launch {
        runCatching {
            require(link.title.isNotBlank()) { "Link title is required." }
            require(link.url.startsWith("http://") || link.url.startsWith("https://")) { "Use an http:// or https:// URL." }
            repository.saveLink(link)
        }.onSuccess { onDone("Link saved.") }
            .onFailure { onDone(it.message ?: "Could not save link.") }
    }

    fun deleteLink(id: Long) = viewModelScope.launch { repository.deleteLink(id) }
    fun savePhoto(photo: PhotoEntryEntity, onDone: (String) -> Unit = {}) = viewModelScope.launch {
        runCatching {
            require(photo.title.isNotBlank()) { "Photo title is required." }
            require(photo.uri.isNotBlank()) { "Choose or take a photo first." }
            repository.savePhoto(photo)
        }.onSuccess { onDone("Photo saved.") }
            .onFailure { onDone(it.message ?: "Could not save photo.") }
    }

    fun deletePhoto(id: Long) = viewModelScope.launch { repository.deletePhoto(id) }
    fun saveShoppingItem(item: ShoppingItemEntity) = viewModelScope.launch { repository.saveShoppingItem(item) }
    fun deleteShoppingItem(id: Long) = viewModelScope.launch { repository.deleteShoppingItem(id) }
    fun clearCompletedShopping() = viewModelScope.launch { repository.clearCompletedShopping() }
}
