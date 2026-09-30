package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "package_cache")
data class PackageCacheEntity(
    @PrimaryKey
    val packageName: String,
    val version: String,
    val sizeBytes: Long,
    val localFilePath: String,
    val isAndroidCompatible: Boolean = true,
    val requiresNativeRecipe: Boolean = false,
    val cachedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "import_mappings")
data class ImportMappingEntity(
    @PrimaryKey
    val importName: String,
    val suggestedPackage: String,
    val confidence: String, // CONFIRMED, LIKELY, UNKNOWN
    val isAndroidCompatible: Boolean = true,
    val requiresNativeRecipe: Boolean = false,
    val notes: String = "",
    val isCustomOrUserCorrected: Boolean = false
)
