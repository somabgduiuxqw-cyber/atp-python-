package com.example.engine.analyzer

import com.example.data.model.*
import java.io.File

class DependencyResolver(
    private val projectDir: File,
    private val installedPackages: Set<String> = emptySet()
) {
    /**
     * Resolves all dependencies by inspecting code imports, requirements.txt, and metadata.
     */
    fun resolve(): DependencyResolution {
        val scanner = PythonProjectScanner(projectDir)
        val allRawImports = scanner.scanProject()

        // Deduplicate imports by root module name, merging flags
        val groupedImports = allRawImports.groupBy { it.moduleName }
        val mergedImports = groupedImports.map { (modName, list) ->
            val first = list.first()
            val anyConditional = list.any { it.isConditional }
            val anyDynamic = list.any { it.isDynamic }
            val allSubmodules = list.flatMap { it.submodulesOrClasses }.distinct()

            first.copy(
                isConditional = anyConditional,
                isDynamic = anyDynamic,
                submodulesOrClasses = allSubmodules,
                status = when {
                    first.confidence == ImportConfidence.BUILTIN -> DependencyStatus.BUILTIN
                    first.confidence == ImportConfidence.LOCAL -> DependencyStatus.LOCAL_MODULE
                    first.mappedPackage != null && installedPackages.contains(first.mappedPackage!!.lowercase()) -> DependencyStatus.INSTALLED
                    else -> DependencyStatus.MISSING
                }
            )
        }

        val required = mutableListOf<DetectedImport>()
        val suggested = mutableListOf<DetectedImport>()
        val unknown = mutableListOf<DetectedImport>()
        val standardLibrary = mutableListOf<DetectedImport>()
        val localModules = mutableListOf<DetectedImport>()

        for (item in mergedImports) {
            when (item.confidence) {
                ImportConfidence.BUILTIN -> standardLibrary.add(item)
                ImportConfidence.LOCAL -> localModules.add(item)
                ImportConfidence.CONFIRMED -> {
                    if (item.isConditional) {
                        suggested.add(item.copy(notes = "Conditional import: ${item.notes}"))
                    } else {
                        required.add(item)
                    }
                }
                ImportConfidence.LIKELY -> suggested.add(item)
                ImportConfidence.UNKNOWN -> unknown.add(item)
            }
        }

        // Parse requirements.txt
        val requirementsFile = File(projectDir, "requirements.txt")
        val declaredRequirements = parseRequirementsFile(requirementsFile)

        // Find unused dependencies in requirements.txt (declared in requirements.txt but never imported in code)
        val importedPackageNames = (required + suggested + unknown)
            .mapNotNull { it.mappedPackage?.lowercase() ?: it.moduleName.lowercase() }
            .toSet()

        val unusedInRequirements = mutableListOf<UnusedDependency>()
        for (req in declaredRequirements) {
            val reqNameLower = req.packageName.lowercase()
            if (!importedPackageNames.contains(reqNameLower) && !PythonStdLib.isStdLib(reqNameLower)) {
                unusedInRequirements.add(
                    UnusedDependency(
                        packageName = req.rawLine,
                        definedIn = "requirements.txt",
                        estimatedSizeImpact = calculateEstimatedImpact(req.packageName)
                    )
                )
            }
        }

        // Detect conflicts (e.g. conflicting version requirements or known incompatible combos)
        val conflicts = detectConflicts(declaredRequirements, required)

        return DependencyResolution(
            required = required,
            suggested = suggested,
            unknown = unknown,
            standardLibrary = standardLibrary,
            localModules = localModules,
            unusedInRequirements = unusedInRequirements,
            conflicts = conflicts,
            allImports = mergedImports
        )
    }

    data class ParsedRequirement(
        val packageName: String,
        val versionSpecifier: String,
        val rawLine: String
    )

    private fun parseRequirementsFile(file: File): List<ParsedRequirement> {
        if (!file.exists() || !file.isFile) return emptyList()
        val results = mutableListOf<ParsedRequirement>()
        val lines = try { file.readLines() } catch (e: Exception) { return emptyList() }

        val specifierRegex = Regex("""^([a-zA-Z0-9_\-\.]+)(?:([=><~^!].*))?$""")
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue
            val clean = trimmed.split("#").first().trim()
            val match = specifierRegex.find(clean)
            if (match != null) {
                val pkgName = match.groupValues[1]
                val versionSpec = if (match.groupValues.size > 2) match.groupValues[2] else ""
                results.add(ParsedRequirement(pkgName, versionSpec, clean))
            }
        }
        return results
    }

    private fun calculateEstimatedImpact(packageName: String): String {
        return when (packageName.lowercase()) {
            "numpy" -> "~28 MB (native C/Fortran extension)"
            "pandas" -> "~35 MB"
            "scipy" -> "~45 MB"
            "pillow" -> "~5.5 MB"
            "requests" -> "~1.2 MB"
            "kivy" -> "~18 MB"
            "kivymd" -> "~9 MB"
            "python-telegram-bot" -> "~4 MB"
            else -> "~2.5 MB"
        }
    }

    private fun detectConflicts(
        requirements: List<ParsedRequirement>,
        requiredImports: List<DetectedImport>
    ): List<DependencyConflict> {
        val conflicts = mutableListOf<DependencyConflict>()
        for (req in requirements) {
            if (req.packageName.equals("opencv-python", ignoreCase = true) && req.versionSpecifier.isEmpty()) {
                conflicts.add(
                    DependencyConflict(
                        packageName = req.packageName,
                        requestedVersion = "latest",
                        requiredBy = "requirements.txt",
                        conflictReason = "Pre-compiled desktop wheels for opencv-python are not directly installable on Android. Requires opencv recipe via python-for-android."
                    )
                )
            }
        }
        return conflicts
    }

    /**
     * Estimates APK size based on detected dependencies and framework choice.
     */
    fun calculateEstimatedApkSize(
        framework: FrameworkType,
        confirmedPackages: List<String>
    ): EstimatedApkSize {
        var runtimeBytes = 14_500_000L // base libpython3.so + stdlib zip
        var frameworkBytes = when (framework) {
            FrameworkType.KIVY -> 8_800_000L
            FrameworkType.KIVYMD -> 16_500_000L
            FrameworkType.BEEWARE, FrameworkType.BRIEFCASE -> 7_200_000L
            FrameworkType.PYTHON_FOR_ANDROID -> 5_500_000L
            FrameworkType.AUTO -> 9_000_000L
        }

        var packagesBytes = 0L
        for (pkg in confirmedPackages) {
            packagesBytes += when (pkg.lowercase()) {
                "pillow" -> 5_200_000L
                "requests" -> 1_100_000L
                "python-telegram-bot" -> 3_800_000L
                "numpy" -> 26_000_000L
                "pandas" -> 32_000_000L
                "kivymd" -> 8_500_000L
                "httpx" -> 1_500_000L
                "pyyaml" -> 1_800_000L
                "beautifulsoup4" -> 1_200_000L
                else -> 900_000L
            }
        }

        val appSourceBytes = 250_000L
        val appAssetsBytes = 1_800_000L
        val nativeLibrariesBytes = 7_500_000L

        val total = runtimeBytes + frameworkBytes + packagesBytes + appSourceBytes + appAssetsBytes + nativeLibrariesBytes

        return EstimatedApkSize(
            pythonRuntimeBytes = runtimeBytes,
            frameworkBytes = frameworkBytes,
            packagesBytes = packagesBytes,
            appSourceBytes = appSourceBytes,
            appAssetsBytes = appAssetsBytes,
            nativeLibrariesBytes = nativeLibrariesBytes,
            totalEstimatedBytes = total
        )
    }

    /**
     * Generates synced requirements.txt content from confirmed packages.
     */
    fun generateSyncedRequirements(
        currentFileContent: String,
        packagesToAdd: List<String>,
        packagesToRemove: List<String>
    ): String {
        val existingLines = currentFileContent.lines().toMutableList()
        val toRemoveLower = packagesToRemove.map { it.lowercase() }.toSet()

        // Filter out removals
        val filtered = existingLines.filter { line ->
            val pkg = line.trim().split("=").first().split(">").first().split("<").first().trim().lowercase()
            !toRemoveLower.contains(pkg)
        }.toMutableList()

        val existingPkgNames = filtered.mapNotNull {
            val t = it.trim()
            if (t.isEmpty() || t.startsWith("#")) null
            else t.split("=").first().split(">").first().split("<").first().trim().lowercase()
        }.toSet()

        // Append new additions
        for (pkg in packagesToAdd) {
            if (!existingPkgNames.contains(pkg.lowercase())) {
                filtered.add(pkg)
            }
        }

        return filtered.joinToString("\n").trim() + "\n"
    }
}
