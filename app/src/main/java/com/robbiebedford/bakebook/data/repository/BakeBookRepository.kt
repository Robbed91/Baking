package com.robbiebedford.bakebook.data.repository

import com.robbiebedford.bakebook.data.database.BakeBookDatabase
import com.robbiebedford.bakebook.data.database.AchievementEntity
import com.robbiebedford.bakebook.data.database.BakeLogEntity
import com.robbiebedford.bakebook.data.database.OccasionEntity
import com.robbiebedford.bakebook.data.database.OccasionRecipeLinkEntity
import com.robbiebedford.bakebook.data.database.PantryItemEntity
import com.robbiebedford.bakebook.data.database.PhotoEntryEntity
import com.robbiebedford.bakebook.data.database.RecipeCollectionEntity
import com.robbiebedford.bakebook.data.database.RecipeCollectionLinkEntity
import com.robbiebedford.bakebook.data.database.RecipeEntity
import com.robbiebedford.bakebook.data.database.RecipeLinkEntity
import com.robbiebedford.bakebook.data.database.RecipeWithDetails
import com.robbiebedford.bakebook.data.database.ShoppingItemEntity
import com.robbiebedford.bakebook.data.database.SubstitutionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class BakeBookRepository(private val db: BakeBookDatabase) {
    val recipes: Flow<List<RecipeEntity>> = db.recipeDao().observeRecipes()
    val recentRecipes: Flow<List<RecipeEntity>> = db.recipeDao().observeRecentRecipes()
    val links: Flow<List<RecipeLinkEntity>> = db.linkDao().observeLinks()
    val photos: Flow<List<PhotoEntryEntity>> = db.photoDao().observePhotos()
    val shoppingItems: Flow<List<ShoppingItemEntity>> = db.shoppingDao().observeItems()
    val collections: Flow<List<RecipeCollectionEntity>> = db.extraDao().observeCollections()
    val collectionLinks: Flow<List<RecipeCollectionLinkEntity>> = db.extraDao().observeCollectionLinks()
    val bakeLogs: Flow<List<BakeLogEntity>> = db.extraDao().observeBakeLogs()
    val pantryItems: Flow<List<PantryItemEntity>> = db.extraDao().observePantryItems()
    val occasions: Flow<List<OccasionEntity>> = db.extraDao().observeOccasions()
    val occasionLinks: Flow<List<OccasionRecipeLinkEntity>> = db.extraDao().observeOccasionLinks()
    val substitutions: Flow<List<SubstitutionEntity>> = db.extraDao().observeSubstitutions()
    val achievements: Flow<List<AchievementEntity>> = db.extraDao().observeAchievements()

    suspend fun recipeDetails(id: Long): RecipeWithDetails? {
        val recipe = db.recipeDao().getRecipe(id) ?: return null
        return RecipeWithDetails(recipe, db.recipeDao().getIngredients(id), db.recipeDao().getSteps(id))
    }

    suspend fun saveRecipe(recipe: RecipeEntity, ingredients: List<String>, steps: List<String>) =
        db.recipeDao().saveRecipe(recipe, ingredients, steps)

    suspend fun updateRecipe(recipe: RecipeEntity) = db.recipeDao().updateRecipe(recipe.copy(dateUpdated = System.currentTimeMillis()))

    suspend fun duplicateRecipe(id: Long) {
        val detail = recipeDetails(id) ?: return
        saveRecipe(
            detail.recipe.copy(id = 0, title = "${detail.recipe.title} Copy", dateCreated = System.currentTimeMillis()),
            detail.ingredients.map { it.text },
            detail.steps.map { it.text }
        )
    }

    suspend fun deleteRecipe(id: Long) = db.recipeDao().deleteRecipe(id)

    suspend fun generateShoppingList(recipeId: Long) {
        val details = recipeDetails(recipeId) ?: return
        val pantry = pantryItems.first()
        db.shoppingDao().saveAll(details.ingredients.map { ingredient ->
            val pantryMatch = pantry.firstOrNull { ingredient.text.contains(it.name, ignoreCase = true) || it.name.contains(ingredient.text.takeWhile { ch -> ch.isLetter() }, ignoreCase = true) }
            val status = when {
                pantryMatch == null -> "Need to buy"
                pantryMatch.lowStock -> "Need to buy - low stock"
                else -> "You may already have this"
            }
            ShoppingItemEntity(name = ingredient.text, status = status, sourceRecipeId = recipeId)
        })
    }

    suspend fun saveLink(link: RecipeLinkEntity) = db.linkDao().save(link)
    suspend fun deleteLink(id: Long) = db.linkDao().delete(id)
    suspend fun savePhoto(photo: PhotoEntryEntity) = db.photoDao().save(photo)
    suspend fun deletePhoto(id: Long) = db.photoDao().delete(id)
    suspend fun saveShoppingItem(item: ShoppingItemEntity) = db.shoppingDao().save(item)
    suspend fun deleteShoppingItem(id: Long) = db.shoppingDao().delete(id)
    suspend fun clearCompletedShopping() = db.shoppingDao().clearCompleted()
    suspend fun saveCollection(collection: RecipeCollectionEntity) = db.extraDao().saveCollection(collection)
    suspend fun deleteCollection(id: Long) = db.extraDao().deleteCollection(id)
    suspend fun saveRecipeCollectionLink(link: RecipeCollectionLinkEntity) = db.extraDao().saveRecipeCollectionLink(link)
    suspend fun deleteRecipeCollectionLink(recipeId: Long, collectionId: Long) = db.extraDao().deleteRecipeCollectionLink(recipeId, collectionId)
    suspend fun saveBakeLog(log: BakeLogEntity) = db.extraDao().saveBakeLog(log)
    suspend fun deleteBakeLog(id: Long) = db.extraDao().deleteBakeLog(id)
    fun bakeLogsForRecipe(recipeId: Long): Flow<List<BakeLogEntity>> = db.extraDao().observeBakeLogsForRecipe(recipeId)
    fun photosForRecipe(recipeId: Long): Flow<List<PhotoEntryEntity>> = db.photoDao().observePhotosForRecipe(recipeId)
    suspend fun savePantryItem(item: PantryItemEntity) = db.extraDao().savePantryItem(item)
    suspend fun deletePantryItem(id: Long) = db.extraDao().deletePantryItem(id)
    suspend fun saveOccasion(occasion: OccasionEntity) = db.extraDao().saveOccasion(occasion)
    suspend fun deleteOccasion(id: Long) = db.extraDao().deleteOccasion(id)
    suspend fun saveOccasionRecipeLink(link: OccasionRecipeLinkEntity) = db.extraDao().saveOccasionRecipeLink(link)
    suspend fun deleteOccasionRecipeLink(occasionId: Long, recipeId: Long) = db.extraDao().deleteOccasionRecipeLink(occasionId, recipeId)
    suspend fun saveSubstitution(substitution: SubstitutionEntity) = db.extraDao().saveSubstitution(substitution)
    suspend fun deleteSubstitution(id: Long) = db.extraDao().deleteSubstitution(id)
    suspend fun unlockAchievement(achievement: AchievementEntity) = db.extraDao().saveAchievement(achievement)
}
