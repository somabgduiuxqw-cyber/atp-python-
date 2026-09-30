package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.entity.BuildHistoryEntity
import com.example.data.entity.ImportMappingEntity
import com.example.data.entity.PackageCacheEntity
import com.example.data.entity.ProjectEntity

@Database(
    entities = [
        ProjectEntity::class,
        BuildHistoryEntity::class,
        PackageCacheEntity::class,
        ImportMappingEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun buildHistoryDao(): BuildHistoryDao
    abstract fun packageCacheDao(): PackageCacheDao
    abstract fun importMappingDao(): ImportMappingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "atp_python.db"
                ).fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
