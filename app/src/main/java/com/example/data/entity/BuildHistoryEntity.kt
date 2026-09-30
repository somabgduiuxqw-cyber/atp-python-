package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "build_history")
data class BuildHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long,
    val projectName: String,
    val backend: String,
    val buildType: String, // DEBUG, RELEASE
    val status: String, // SUCCESS, FAILED, RUNNING, CANCELLED
    val apkPath: String? = null,
    val apkSize: Long = 0L,
    val durationMs: Long = 0L,
    val errorSummary: String? = null,
    val fullLogs: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
