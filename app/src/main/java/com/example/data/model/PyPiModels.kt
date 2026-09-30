package com.example.data.model

data class PyPiPackageInfo(
    val name: String,
    val version: String,
    val summary: String,
    val author: String = "",
    val license: String = "",
    val homePage: String = "",
    val requiresDist: List<String> = emptyList(),
    val isAndroidCompatible: Boolean = true,
    val nativeRequirements: String = "Pure Python",
    val requiresRecipe: Boolean = false,
    val downloadUrl: String? = null,
    val sizeBytes: Long = 0L
)

data class PackageInstallProgress(
    val packageName: String,
    val stage: String, // DOWNLOADING, RESOLVING, INSTALLING, VERIFYING, COMPLETED, FAILED
    val progressPercent: Float = 0f,
    val currentLog: String = "",
    val error: String? = null
)

data class DependencyTreeNode(
    val name: String,
    val version: String,
    val children: List<DependencyTreeNode> = emptyList()
)
