package com.robbiebedford.bakebook.data.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class RecipeCategory {
    Cakes, Cupcakes, Cookies, Brownies, Bread, Pastry, Desserts, Traybakes, Cheesecakes, Other
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
    val dateBaked: Long = System.currentTimeMillis()
)

@Entity(tableName = "shopping_items")
data class ShoppingItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val complete: Boolean = false,
    val sourceRecipeId: Long? = null
)

data class RecipeWithDetails(
    val recipe: RecipeEntity,
    val ingredients: List<RecipeIngredientEntity>,
    val steps: List<RecipeStepEntity>
)
