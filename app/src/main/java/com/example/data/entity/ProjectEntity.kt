package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val directoryPath: String,
    val framework: String = "KIVY", // KIVY, KIVYMD, BEEWARE, BRIEFCASE, PYTHON_FOR_ANDROID, AUTO
    val appName: String,
    val packageName: String,
    val versionName: String = "1.0.0",
    val versionCode: Int = 1,
    val orientation: String = "PORTRAIT", // PORTRAIT, LANDSCAPE, SENSOR
    val permissionsJson: String = "[\"android.permission.INTERNET\"]",
    val backgroundMode: String = "NONE", // NONE, BACKGROUND_WORK, FOREGROUND_SERVICE
    val creatorName: String = "",
    val creatorUsername: String = "",
    val showBranding: Boolean = true,
    val showCreator: Boolean = true,
    val logoPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis()
)
