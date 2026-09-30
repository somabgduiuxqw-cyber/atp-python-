package com.example.data.model

enum class FrameworkType(val displayName: String, val description: String) {
    KIVY("Kivy", "OpenGL ES powered cross-platform GUI framework"),
    KIVYMD("KivyMD", "Material Design widgets for Kivy"),
    BEEWARE("BeeWare / Briefcase", "Native widget toolkit powered by Toga & Briefcase"),
    BRIEFCASE("Briefcase", "BeeWare packaging tool for mobile"),
    PYTHON_FOR_ANDROID("python-for-android", "Direct p4a bootstrap packaging"),
    AUTO("Auto Detect", "Automatically detect framework from project imports")
}

enum class ImportConfidence {
    CONFIRMED, // Known exact provider (e.g. requests, PIL->Pillow, yaml->PyYAML)
    LIKELY,    // Highly probable provider
    UNKNOWN,   // Unknown or ambiguous (e.g. color -> requires search/selection)
    BUILTIN,   // Python standard library (os, sys, json) -> no package required
    LOCAL      // Local project module (e.g. color.py in project) -> no external package required
}

enum class DependencyStatus {
    INSTALLED,
    MISSING,
    CONFLICT,
    BUILTIN,
    LOCAL_MODULE
}

data class DetectedImport(
    val moduleName: String,
    val sourceFile: String,
    val lineNumber: Int,
    val rawStatement: String,
    val submodulesOrClasses: List<String> = emptyList(),
    val isRelative: Boolean = false,
    val isConditional: Boolean = false, // inside try/except or if statement
    val isDynamic: Boolean = false,     // importlib.import_module or __import__
    var confidence: ImportConfidence = ImportConfidence.UNKNOWN,
    var mappedPackage: String? = null,
    var status: DependencyStatus = DependencyStatus.MISSING,
    var candidatePackages: List<CandidatePackage> = emptyList(),
    var isAndroidCompatible: Boolean = true,
    var requiresNativeRecipe: Boolean = false,
    var notes: String = ""
)

data class CandidatePackage(
    val name: String,
    val summary: String,
    val version: String = "",
    val confidenceScore: Float = 0.5f,
    val isAndroidCompatible: Boolean = true,
    val nativeRequirements: String = "None"
)

data class UnusedDependency(
    val packageName: String,
    val definedIn: String = "requirements.txt",
    val estimatedSizeImpact: String = "~2 MB"
)

data class DependencyConflict(
    val packageName: String,
    val requestedVersion: String,
    val requiredBy: String,
    val conflictReason: String
)

data class DependencyResolution(
    val required: List<DetectedImport>,
    val suggested: List<DetectedImport>,
    val unknown: List<DetectedImport>,
    val standardLibrary: List<DetectedImport>,
    val localModules: List<DetectedImport>,
    val unusedInRequirements: List<UnusedDependency>,
    val conflicts: List<DependencyConflict>,
    val allImports: List<DetectedImport>
)

data class EstimatedApkSize(
    val pythonRuntimeBytes: Long = 14_000_000L, // ~14MB
    val frameworkBytes: Long = 8_500_000L,      // Kivy / BeeWare ~8.5MB
    val packagesBytes: Long = 4_200_000L,       // third party packages ~4.2MB
    val appSourceBytes: Long = 150_000L,        // user code ~150KB
    val appAssetsBytes: Long = 2_100_000L,      // assets & drawables ~2.1MB
    val nativeLibrariesBytes: Long = 6_500_000L,// libpython, libffi, etc. ~6.5MB
    val totalEstimatedBytes: Long = 35_450_000L
) {
    fun formattedTotal(): String = String.format("%.1f MB", totalEstimatedBytes / (1024.0 * 1024.0))
}
