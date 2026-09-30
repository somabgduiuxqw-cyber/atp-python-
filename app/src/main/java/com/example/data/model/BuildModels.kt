package com.example.data.model

import java.io.File

enum class BuildMode {
    LOCAL,
    ONLINE
}

enum class StepState {
    PENDING,
    RUNNING,
    SUCCESS,
    FAILED,
    SKIPPED
}

data class BuildStep(
    val id: String,
    val title: String,
    val description: String,
    var state: StepState = StepState.PENDING,
    var logOutput: String = ""
)

data class ApkVerificationCheck(
    val name: String,
    val passed: Boolean,
    val details: String
)

data class ApkVerificationResult(
    val isValid: Boolean,
    val checks: List<ApkVerificationCheck>,
    val apkFile: File?,
    val apkSize: Long,
    val sha256Checksum: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Int,
    val failureReason: String? = null
)

data class ToolStatus(
    val id: String,
    val name: String,
    val command: String,
    val isInstalled: Boolean,
    val detectedVersion: String,
    val expectedRequirement: String,
    val notes: String = "",
    val isCompatible: Boolean = true
)

data class DeviceResourceReport(
    val totalRamMb: Long,
    val availableRamMb: Long,
    val totalStorageMb: Long,
    val availableStorageMb: Long,
    val primaryAbi: String,
    val supportedAbis: List<String>,
    val androidVersion: String,
    val apiLevel: Int,
    val isLocalBuildViable: Boolean,
    val recommendation: String
)

data class OnlineBuildJob(
    val buildId: String,
    val serverUrl: String,
    val status: String, // QUEUED, BUILDING, COMPLETED, FAILED, CANCELLED
    val queuePosition: Int? = null,
    val progressPercent: Int = 0,
    val logs: List<String> = emptyList(),
    val downloadUrl: String? = null,
    val error: String? = null
)
