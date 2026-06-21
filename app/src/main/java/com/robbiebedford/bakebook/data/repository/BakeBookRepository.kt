package com.robbiebedford.bakebook.data.repository

import com.robbiebedford.bakebook.data.database.BakeBookDatabase
import com.robbiebedford.bakebook.data.database.PhotoEntryEntity
import com.robbiebedford.bakebook.data.database.RecipeEntity
import com.robbiebedford.bakebook.data.database.RecipeLinkEntity
import com.robbiebedford.bakebook.data.database.RecipeWithDetails
import com.robbiebedford.bakebook.data.database.ShoppingItemEntity
import kotlinx.coroutines.flow.Flow

class BakeBookRepository(private val db: BakeBookDatabase) {
    val recipes: Flow<List<RecipeEntity>> = db.recipeDao().observeRecipes()
    val links: Flow<List<RecipeLinkEntity>> = db.linkDao().observeLinks()
    val photos: Flow<List<PhotoEntryEntity>> = db.photoDao().observePhotos()
    val shoppingItems: Flow<List<ShoppingItemEntity>> = db.shoppingDao().observeItems()

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
        db.shoppingDao().saveAll(details.ingredients.map { ShoppingItemEntity(name = it.text, sourceRecipeId = recipeId) })
    }

    suspend fun saveLink(link: RecipeLinkEntity) = db.linkDao().save(link)
    suspend fun deleteLink(id: Long) = db.linkDao().delete(id)
    suspend fun savePhoto(photo: PhotoEntryEntity) = db.photoDao().save(photo)
    suspend fun deletePhoto(id: Long) = db.photoDao().delete(id)
    suspend fun saveShoppingItem(item: ShoppingItemEntity) = db.shoppingDao().save(item)
    suspend fun deleteShoppingItem(id: Long) = db.shoppingDao().delete(id)
    suspend fun clearCompletedShopping() = db.shoppingDao().clearCompleted()
}
