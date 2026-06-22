package com.robbiebedford.bakebook.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.robbiebedford.bakebook.data.database.AchievementEntity
import com.robbiebedford.bakebook.data.database.BakeLogEntity
import com.robbiebedford.bakebook.data.database.OccasionEntity
import com.robbiebedford.bakebook.data.database.OccasionRecipeLinkEntity
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
    val difficulty: String = RecipeDifficulty.Easy.name,
    val favourite: Boolean = false
)

class BakeBookViewModel(private val repository: BakeBookRepository) : ViewModel() {
    val recipes: StateFlow<List<RecipeEntity>> = repository.recipes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val links: StateFlow<List<RecipeLinkEntity>> = repository.links.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val photos: StateFlow<List<PhotoEntryEntity>> = repository.photos.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val shoppingItems: StateFlow<List<ShoppingItemEntity>> = repository.shoppingItems.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val collections: StateFlow<List<RecipeCollectionEntity>> = repository.collections.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val collectionLinks: StateFlow<List<RecipeCollectionLinkEntity>> = repository.collectionLinks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val bakeLogs: StateFlow<List<BakeLogEntity>> = repository.bakeLogs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val pantryItems: StateFlow<List<PantryItemEntity>> = repository.pantryItems.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val occasions: StateFlow<List<OccasionEntity>> = repository.occasions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val occasionLinks: StateFlow<List<OccasionRecipeLinkEntity>> = repository.occasionLinks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val substitutions: StateFlow<List<SubstitutionEntity>> = repository.substitutions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val achievements: StateFlow<List<AchievementEntity>> = repository.achievements.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
                    difficulty = form.difficulty,
                    favourite = form.favourite
                ),
                form.ingredients.lines(),
                form.method.lines()
            )
        }.onSuccess {
            unlockAchievement(AchievementEntity("first_recipe", "First recipe saved"))
            onDone("Recipe saved.")
        }
            .onFailure { onDone(it.message ?: "Could not save recipe.") }
    }

    fun toggleFavourite(recipe: RecipeEntity) = viewModelScope.launch {
        repository.updateRecipe(recipe.copy(favourite = !recipe.favourite))
    }

    fun updateRecipe(recipe: RecipeEntity) = viewModelScope.launch { repository.updateRecipe(recipe) }

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
    fun saveCollection(collection: RecipeCollectionEntity) = viewModelScope.launch {
        repository.saveCollection(collection)
        unlockAchievement(AchievementEntity("first_collection", "First collection created"))
    }
    fun deleteCollection(id: Long) = viewModelScope.launch { repository.deleteCollection(id) }
    fun addRecipeToCollection(recipeId: Long, collectionId: Long) = viewModelScope.launch { repository.saveRecipeCollectionLink(RecipeCollectionLinkEntity(recipeId, collectionId)) }
    fun removeRecipeFromCollection(recipeId: Long, collectionId: Long) = viewModelScope.launch { repository.deleteRecipeCollectionLink(recipeId, collectionId) }
    fun saveBakeLog(log: BakeLogEntity) = viewModelScope.launch {
        repository.saveBakeLog(log)
        unlockAchievement(AchievementEntity("first_bake_log", "First bake log added"))
    }
    fun deleteBakeLog(id: Long) = viewModelScope.launch { repository.deleteBakeLog(id) }
    fun savePantryItem(item: PantryItemEntity) = viewModelScope.launch { repository.savePantryItem(item) }
    fun deletePantryItem(id: Long) = viewModelScope.launch { repository.deletePantryItem(id) }
    fun saveOccasion(occasion: OccasionEntity) = viewModelScope.launch { repository.saveOccasion(occasion) }
    fun deleteOccasion(id: Long) = viewModelScope.launch { repository.deleteOccasion(id) }
    fun addRecipeToOccasion(occasionId: Long, recipeId: Long) = viewModelScope.launch { repository.saveOccasionRecipeLink(OccasionRecipeLinkEntity(occasionId, recipeId)) }
    fun removeRecipeFromOccasion(occasionId: Long, recipeId: Long) = viewModelScope.launch { repository.deleteOccasionRecipeLink(occasionId, recipeId) }
    fun saveSubstitution(substitution: SubstitutionEntity) = viewModelScope.launch { repository.saveSubstitution(substitution) }
    fun deleteSubstitution(id: Long) = viewModelScope.launch { repository.deleteSubstitution(id) }
    fun unlockAchievement(achievement: AchievementEntity) = viewModelScope.launch { repository.unlockAchievement(achievement) }
}
