package com.robbiebedford.bakebook.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {
    @Query("SELECT * FROM recipes ORDER BY favourite DESC, dateUpdated DESC")
    fun observeRecipes(): Flow<List<RecipeEntity>>

    @Query("SELECT * FROM recipes WHERE id = :id")
    suspend fun getRecipe(id: Long): RecipeEntity?

    @Query("SELECT * FROM recipe_ingredients WHERE recipeId = :recipeId ORDER BY sortOrder")
    suspend fun getIngredients(recipeId: Long): List<RecipeIngredientEntity>

    @Query("SELECT * FROM recipe_steps WHERE recipeId = :recipeId ORDER BY sortOrder")
    suspend fun getSteps(recipeId: Long): List<RecipeStepEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: RecipeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredients(ingredients: List<RecipeIngredientEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSteps(steps: List<RecipeStepEntity>)

    @Update
    suspend fun updateRecipe(recipe: RecipeEntity)

    @Query("DELETE FROM recipe_ingredients WHERE recipeId = :recipeId")
    suspend fun deleteIngredients(recipeId: Long)

    @Query("DELETE FROM recipe_steps WHERE recipeId = :recipeId")
    suspend fun deleteSteps(recipeId: Long)

    @Query("DELETE FROM recipes WHERE id = :id")
    suspend fun deleteRecipe(id: Long)

    @Transaction
    suspend fun saveRecipe(recipe: RecipeEntity, ingredients: List<String>, steps: List<String>): Long {
        val id = if (recipe.id == 0L) insertRecipe(recipe) else {
            updateRecipe(recipe.copy(dateUpdated = System.currentTimeMillis()))
            recipe.id
        }
        deleteIngredients(id)
        deleteSteps(id)
        insertIngredients(ingredients.filter { it.isNotBlank() }.mapIndexed { index, text ->
            RecipeIngredientEntity(recipeId = id, text = text.trim(), sortOrder = index)
        })
        insertSteps(steps.filter { it.isNotBlank() }.mapIndexed { index, text ->
            RecipeStepEntity(recipeId = id, text = text.trim(), sortOrder = index)
        })
        return id
    }
}

@Dao
interface LinkDao {
    @Query("SELECT * FROM recipe_links ORDER BY dateCreated DESC")
    fun observeLinks(): Flow<List<RecipeLinkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(link: RecipeLinkEntity): Long

    @Query("DELETE FROM recipe_links WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface PhotoDao {
    @Query("SELECT * FROM photos ORDER BY dateBaked DESC")
    fun observePhotos(): Flow<List<PhotoEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(photo: PhotoEntryEntity): Long

    @Query("DELETE FROM photos WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface ShoppingDao {
    @Query("SELECT * FROM shopping_items ORDER BY complete ASC, id DESC")
    fun observeItems(): Flow<List<ShoppingItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(item: ShoppingItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAll(items: List<ShoppingItemEntity>)

    @Query("DELETE FROM shopping_items WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM shopping_items WHERE complete = 1")
    suspend fun clearCompleted()
}
