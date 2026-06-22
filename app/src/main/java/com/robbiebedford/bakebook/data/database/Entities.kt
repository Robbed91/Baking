package com.robbiebedford.bakebook.data.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class RecipeCategory {
    Cakes, Cupcakes, Cookies, Brownies, Bread, Pastry, Desserts, Traybakes, Cheesecakes, Other
}

enum class RecipeDifficulty {
    Easy, Medium, Hard, Showstopper
}

@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String = RecipeCategory.Other.name,
    val prepTime: String = "",
    val bakeTime: String = "",
    val ovenTemperature: String = "",
    val servings: String = "",
    val notes: String = "",
    val rating: Int = 0,
    val difficulty: String = RecipeDifficulty.Easy.name,
    val coverPhotoUri: String = "",
    val favourite: Boolean = false,
    val dateCreated: Long = System.currentTimeMillis(),
    val dateUpdated: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "recipe_ingredients",
    foreignKeys = [ForeignKey(
        entity = RecipeEntity::class,
        parentColumns = ["id"],
        childColumns = ["recipeId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("recipeId")]
)
data class RecipeIngredientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipeId: Long,
    val text: String,
    val cost: Double = 0.0,
    val sortOrder: Int = 0
)

@Entity(
    tableName = "recipe_steps",
    foreignKeys = [ForeignKey(
        entity = RecipeEntity::class,
        parentColumns = ["id"],
        childColumns = ["recipeId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("recipeId")]
)
data class RecipeStepEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipeId: Long,
    val text: String,
    val sortOrder: Int = 0
)

@Entity(tableName = "recipe_links")
data class RecipeLinkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val websiteName: String = "",
    val url: String,
    val notes: String = "",
    val category: String = RecipeCategory.Other.name,
    val dateCreated: Long = System.currentTimeMillis()
)

@Entity(tableName = "photos")
data class PhotoEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val uri: String,
    val linkedRecipeId: Long? = null,
    val notes: String = "",
    val caption: String = "",
    val isCover: Boolean = false,
    val dateBaked: Long = System.currentTimeMillis()
)

@Entity(tableName = "shopping_items")
data class ShoppingItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val complete: Boolean = false,
    val status: String = "",
    val sourceRecipeId: Long? = null
)

@Entity(tableName = "collections")
data class RecipeCollectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val dateCreated: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "recipe_collection_links",
    primaryKeys = ["recipeId", "collectionId"],
    indices = [Index("recipeId"), Index("collectionId")]
)
data class RecipeCollectionLinkEntity(
    val recipeId: Long,
    val collectionId: Long
)

@Entity(tableName = "bake_logs", indices = [Index("recipeId")])
data class BakeLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipeId: Long,
    val dateBaked: Long = System.currentTimeMillis(),
    val notes: String = "",
    val resultRating: Int = 0,
    val actualBakeTime: String = "",
    val actualOvenTemperature: String = "",
    val changesMade: String = "",
    val linkedPhotoIds: String = ""
)

@Entity(tableName = "pantry_items")
data class PantryItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val quantity: String = "",
    val unit: String = "",
    val expiryDate: Long? = null,
    val lowStock: Boolean = false,
    val minimumQuantity: String = "",
    val notes: String = ""
)

@Entity(tableName = "occasions")
data class OccasionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val date: Long = System.currentTimeMillis(),
    val notes: String = "",
    val bakeSchedule: String = "",
    val completed: Boolean = false
)

@Entity(
    tableName = "occasion_recipe_links",
    primaryKeys = ["occasionId", "recipeId"],
    indices = [Index("occasionId"), Index("recipeId")]
)
data class OccasionRecipeLinkEntity(
    val occasionId: Long,
    val recipeId: Long
)

@Entity(tableName = "substitutions")
data class SubstitutionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ingredient: String,
    val substitute: String,
    val notes: String = "",
    val recipeId: Long? = null
)

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val key: String,
    val title: String,
    val unlockedAt: Long = System.currentTimeMillis()
)

data class RecipeWithDetails(
    val recipe: RecipeEntity,
    val ingredients: List<RecipeIngredientEntity>,
    val steps: List<RecipeStepEntity>
)
