package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        CachedProductEntity::class,
        CachedCategoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MelvoreDatabase : RoomDatabase() {

    abstract fun productCacheDao(): ProductCacheDao

    companion object {
        private const val DATABASE_NAME = "melvore_product_cache.db"

        @Volatile
        private var instance: MelvoreDatabase? = null

        fun getInstance(context: Context): MelvoreDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    MelvoreDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
        }

        /**
         * In-memory database factory for fast, isolated Robolectric / JVM unit tests.
         */
        fun inMemory(context: Context): MelvoreDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                MelvoreDatabase::class.java
            )
                .allowMainThreadQueries()
                .build()
        }
    }
}
