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

    @Query("SELECT * FROM recipes ORDER BY dateCreated DESC LIMIT :limit")
    fun observeRecentRecipes(limit: Int = 5): Flow<List<RecipeEntity>>

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
            val parts = text.split("|", limit = 2)
            RecipeIngredientEntity(
                recipeId = id,
                text = parts.first().trim(),
                cost = parts.getOrNull(1)?.trim()?.toDoubleOrNull() ?: 0.0,
                sortOrder = index
            )
        })
        insertSteps(steps.filter { it.isNotBlank() }.mapIndexed { index, text ->
            RecipeStepEntity(recipeId = id, text = text.trim(), sortOrder = index)
        })
        return id
    }
}

@Dao
interface ExtraDao {
    @Query("SELECT * FROM collections ORDER BY name")
    fun observeCollections(): Flow<List<RecipeCollectionEntity>>

    @Query("SELECT * FROM recipe_collection_links")
    fun observeCollectionLinks(): Flow<List<RecipeCollectionLinkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCollection(collection: RecipeCollectionEntity): Long

    @Query("DELETE FROM collections WHERE id = :id")
    suspend fun deleteCollection(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveRecipeCollectionLink(link: RecipeCollectionLinkEntity)

    @Query("DELETE FROM recipe_collection_links WHERE recipeId = :recipeId AND collectionId = :collectionId")
    suspend fun deleteRecipeCollectionLink(recipeId: Long, collectionId: Long)

    @Query("SELECT * FROM bake_logs ORDER BY dateBaked DESC")
    fun observeBakeLogs(): Flow<List<BakeLogEntity>>

    @Query("SELECT * FROM bake_logs WHERE recipeId = :recipeId ORDER BY dateBaked DESC")
    fun observeBakeLogsForRecipe(recipeId: Long): Flow<List<BakeLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveBakeLog(log: BakeLogEntity): Long

    @Query("DELETE FROM bake_logs WHERE id = :id")
    suspend fun deleteBakeLog(id: Long)

    @Query("SELECT * FROM pantry_items ORDER BY lowStock DESC, name")
    fun observePantryItems(): Flow<List<PantryItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePantryItem(item: PantryItemEntity): Long

    @Query("DELETE FROM pantry_items WHERE id = :id")
    suspend fun deletePantryItem(id: Long)

    @Query("SELECT * FROM occasions ORDER BY completed ASC, date ASC")
    fun observeOccasions(): Flow<List<OccasionEntity>>

    @Query("SELECT * FROM occasion_recipe_links")
    fun observeOccasionLinks(): Flow<List<OccasionRecipeLinkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveOccasion(occasion: OccasionEntity): Long

    @Query("DELETE FROM occasions WHERE id = :id")
    suspend fun deleteOccasion(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveOccasionRecipeLink(link: OccasionRecipeLinkEntity)

    @Query("DELETE FROM occasion_recipe_links WHERE occasionId = :occasionId AND recipeId = :recipeId")
    suspend fun deleteOccasionRecipeLink(occasionId: Long, recipeId: Long)

    @Query("SELECT * FROM substitutions ORDER BY ingredient")
    fun observeSubstitutions(): Flow<List<SubstitutionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSubstitution(substitution: SubstitutionEntity): Long

    @Query("DELETE FROM substitutions WHERE id = :id")
    suspend fun deleteSubstitution(id: Long)

    @Query("SELECT * FROM achievements ORDER BY unlockedAt DESC")
    fun observeAchievements(): Flow<List<AchievementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAchievement(achievement: AchievementEntity)
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

    @Query("SELECT * FROM photos WHERE linkedRecipeId = :recipeId ORDER BY isCover DESC, dateBaked DESC")
    fun observePhotosForRecipe(recipeId: Long): Flow<List<PhotoEntryEntity>>

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
