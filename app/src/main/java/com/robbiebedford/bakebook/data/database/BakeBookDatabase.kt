package com.robbiebedford.bakebook.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        RecipeEntity::class,
        RecipeIngredientEntity::class,
        RecipeStepEntity::class,
        RecipeLinkEntity::class,
        PhotoEntryEntity::class,
        ShoppingItemEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class BakeBookDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao
    abstract fun linkDao(): LinkDao
    abstract fun photoDao(): PhotoDao
    abstract fun shoppingDao(): ShoppingDao

    companion object {
        @Volatile private var instance: BakeBookDatabase? = null

        fun get(context: Context): BakeBookDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    BakeBookDatabase::class.java,
                    "bakebook.db"
                ).build().also { instance = it }
            }
    }
}
