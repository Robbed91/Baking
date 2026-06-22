package com.robbiebedford.bakebook.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        RecipeEntity::class,
        RecipeIngredientEntity::class,
        RecipeStepEntity::class,
        RecipeLinkEntity::class,
        PhotoEntryEntity::class,
        ShoppingItemEntity::class,
        RecipeCollectionEntity::class,
        RecipeCollectionLinkEntity::class,
        BakeLogEntity::class,
        PantryItemEntity::class,
        OccasionEntity::class,
        OccasionRecipeLinkEntity::class,
        SubstitutionEntity::class,
        AchievementEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class BakeBookDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao
    abstract fun linkDao(): LinkDao
    abstract fun photoDao(): PhotoDao
    abstract fun shoppingDao(): ShoppingDao
    abstract fun extraDao(): ExtraDao

    companion object {
        @Volatile private var instance: BakeBookDatabase? = null

        fun get(context: Context): BakeBookDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    BakeBookDatabase::class.java,
                    "bakebook.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE recipes ADD COLUMN difficulty TEXT NOT NULL DEFAULT 'Easy'")
                db.execSQL("ALTER TABLE recipes ADD COLUMN coverPhotoUri TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE recipe_ingredients ADD COLUMN cost REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE photos ADD COLUMN caption TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE photos ADD COLUMN isCover INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE shopping_items ADD COLUMN status TEXT NOT NULL DEFAULT ''")
                db.execSQL("CREATE TABLE IF NOT EXISTS collections (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, dateCreated INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS recipe_collection_links (recipeId INTEGER NOT NULL, collectionId INTEGER NOT NULL, PRIMARY KEY(recipeId, collectionId))")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_recipe_collection_links_recipeId ON recipe_collection_links(recipeId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_recipe_collection_links_collectionId ON recipe_collection_links(collectionId)")
                db.execSQL("CREATE TABLE IF NOT EXISTS bake_logs (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, recipeId INTEGER NOT NULL, dateBaked INTEGER NOT NULL, notes TEXT NOT NULL, resultRating INTEGER NOT NULL, actualBakeTime TEXT NOT NULL, actualOvenTemperature TEXT NOT NULL, changesMade TEXT NOT NULL, linkedPhotoIds TEXT NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_bake_logs_recipeId ON bake_logs(recipeId)")
                db.execSQL("CREATE TABLE IF NOT EXISTS pantry_items (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, quantity TEXT NOT NULL, unit TEXT NOT NULL, expiryDate INTEGER, lowStock INTEGER NOT NULL, minimumQuantity TEXT NOT NULL, notes TEXT NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS occasions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, date INTEGER NOT NULL, notes TEXT NOT NULL, bakeSchedule TEXT NOT NULL, completed INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS occasion_recipe_links (occasionId INTEGER NOT NULL, recipeId INTEGER NOT NULL, PRIMARY KEY(occasionId, recipeId))")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_occasion_recipe_links_occasionId ON occasion_recipe_links(occasionId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_occasion_recipe_links_recipeId ON occasion_recipe_links(recipeId)")
                db.execSQL("CREATE TABLE IF NOT EXISTS substitutions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, ingredient TEXT NOT NULL, substitute TEXT NOT NULL, notes TEXT NOT NULL, recipeId INTEGER)")
                db.execSQL("CREATE TABLE IF NOT EXISTS achievements (`key` TEXT NOT NULL, title TEXT NOT NULL, unlockedAt INTEGER NOT NULL, PRIMARY KEY(`key`))")
            }
        }
    }
}
