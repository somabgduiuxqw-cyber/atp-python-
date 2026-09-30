package com.example.engine.analyzer

import com.example.data.model.CandidatePackage
import com.example.data.model.DependencyStatus
import com.example.data.model.DetectedImport
import com.example.data.model.ImportConfidence
import java.io.File

class PythonProjectScanner(private val projectDir: File) {

    private val localModuleNames = mutableSetOf<String>()

    /**
     * Scans the project directory recursively and analyzes all Python imports.
     */
    fun scanProject(): List<DetectedImport> {
        localModuleNames.clear()
        discoverLocalModules(projectDir, "")

        val pyFiles = projectDir.walkTopDown()
            .filter { it.isFile && it.extension.equals("py", ignoreCase = true) }
            .toList()

        val allDetected = mutableListOf<DetectedImport>()

        for (file in pyFiles) {
            val relativePath = file.relativeTo(projectDir).path
            val fileImports = parsePythonFile(file, relativePath)
            allDetected.addAll(fileImports)
        }

        return allDetected
    }

    /**
     * Discovers all .py files and package directories to recognize user's own modules.
     */
    private fun discoverLocalModules(dir: File, prefix: String) {
        val files = dir.listFiles() ?: return
        for (f in files) {
            if (f.isFile && f.extension.equals("py", ignoreCase = true)) {
                val modName = f.nameWithoutExtension
                localModuleNames.add(modName)
                if (prefix.isNotEmpty()) {
                    localModuleNames.add("$prefix.$modName")
                }
            } else if (f.isDirectory && !f.name.startsWith(".") && f.name != "__pycache__" && f.name != "build") {
                // If it has __init__.py or is a subfolder
                localModuleNames.add(f.name)
                val newPrefix = if (prefix.isEmpty()) f.name else "$prefix.${f.name}"
                discoverLocalModules(f, newPrefix)
            }
        }
    }

    private fun parsePythonFile(file: File, relativePath: String): List<DetectedImport> {
        val results = mutableListOf<DetectedImport>()
        val lines = try { file.readLines() } catch (e: Exception) { return emptyList() }

        var inTryBlock = false
        var tryIndent = -1

        var inIfBlock = false
        var ifIndent = -1

        for ((index, rawLine) in lines.withIndex()) {
            val lineNum = index + 1
            val trimmed = rawLine.trim()

            // Skip comments and empty lines
            if (trimmed.startsWith("#") || trimmed.isEmpty()) continue

            val currentIndent = rawLine.indexOfFirst { !it.isWhitespace() }

            // Track try/except block
            if (trimmed.startsWith("try:")) {
                inTryBlock = true
                tryIndent = currentIndent
            } else if (inTryBlock && (trimmed.startsWith("except") || trimmed.startsWith("finally:") || (currentIndent <= tryIndent && currentIndent != -1))) {
                if (currentIndent <= tryIndent && !trimmed.startsWith("except")) {
                    inTryBlock = false
                    tryIndent = -1
                }
            }

            // Track if block
            if (trimmed.startsWith("if ") && trimmed.endsWith(":")) {
                inIfBlock = true
                ifIndent = currentIndent
            } else if (inIfBlock && currentIndent <= ifIndent && currentIndent != -1) {
                inIfBlock = false
                ifIndent = -1
            }

            val isConditional = inTryBlock || inIfBlock

            // Check dynamic imports: importlib.import_module("...") or __import__("...")
            val dynamicRegex = Regex("""(?:importlib\.import_module|__import__)\(\s*["']([^"']+)["']\s*\)""")
            dynamicRegex.findAll(trimmed).forEach { match ->
                val modName = match.groupValues[1].trim()
                createImportItem(
                    rawModName = modName,
                    sourceFile = relativePath,
                    lineNum = lineNum,
                    rawStatement = trimmed,
                    isRelative = false,
                    isConditional = isConditional,
                    isDynamic = true
                )?.let { results.add(it) }
            }

            // Check: from <module> import <items>
            val fromRegex = Regex("""^from\s+([.\w]+)\s+import\s+(.+)""")
            val fromMatch = fromRegex.find(trimmed)
            if (fromMatch != null) {
                val fullMod = fromMatch.groupValues[1].trim()
                val importedItems = fromMatch.groupValues[2].split(",").map { it.trim().split(" as ").first().trim() }
                val isRelative = fullMod.startsWith(".")

                createImportItem(
                    rawModName = fullMod,
                    sourceFile = relativePath,
                    lineNum = lineNum,
                    rawStatement = trimmed,
                    submodulesOrClasses = importedItems,
                    isRelative = isRelative,
                    isConditional = isConditional,
                    isDynamic = false
                )?.let { results.add(it) }
                continue
            }

            // Check: import <module1> as <alias>, <module2>
            val importRegex = Regex("""^import\s+(.+)""")
            val importMatch = importRegex.find(trimmed)
            if (importMatch != null) {
                val modulesPart = importMatch.groupValues[1]
                val parts = modulesPart.split(",")
                for (part in parts) {
                    val modName = part.trim().split(" as ").first().trim()
                    if (modName.isNotEmpty()) {
                        createImportItem(
                            rawModName = modName,
                            sourceFile = relativePath,
                            lineNum = lineNum,
                            rawStatement = trimmed,
                            isRelative = false,
                            isConditional = isConditional,
                            isDynamic = false
                        )?.let { results.add(it) }
                    }
                }
            }
        }

        return results
    }

    private fun createImportItem(
        rawModName: String,
        sourceFile: String,
        lineNum: Int,
        rawStatement: String,
        submodulesOrClasses: List<String> = emptyList(),
        isRelative: Boolean,
        isConditional: Boolean,
        isDynamic: Boolean
    ): DetectedImport? {
        val cleanName = rawModName.removePrefix(".")
        val rootModule = cleanName.split(".").firstOrNull() ?: return null
        if (rootModule.isEmpty()) return null

        // 1. Relative import -> Guaranteed local project module!
        if (isRelative) {
            return DetectedImport(
                moduleName = rawModName,
                sourceFile = sourceFile,
                lineNumber = lineNum,
                rawStatement = rawStatement,
                submodulesOrClasses = submodulesOrClasses,
                isRelative = true,
                isConditional = isConditional,
                isDynamic = isDynamic,
                confidence = ImportConfidence.LOCAL,
                mappedPackage = null,
                status = DependencyStatus.LOCAL_MODULE,
                isAndroidCompatible = true,
                notes = "Local relative project import"
            )
        }

        // 2. Check if it matches a local module file in project (e.g. color.py in project!)
        if (localModuleNames.contains(rootModule)) {
            // Check if there is also an external package collision
            val externalCandidate = ImportMappingDatabase.findMapping(rootModule, rawStatement, submodulesOrClasses)
            val collisionNote = if (externalCandidate != null && externalCandidate.packageName.isNotEmpty()) {
                "Local module '${rootModule}.py' detected. Python resolves this over PyPI '${externalCandidate.packageName}'."
            } else {
                "Detected local project file: ${rootModule}.py (No external package required)"
            }

            return DetectedImport(
                moduleName = rootModule,
                sourceFile = sourceFile,
                lineNumber = lineNum,
                rawStatement = rawStatement,
                submodulesOrClasses = submodulesOrClasses,
                isRelative = false,
                isConditional = isConditional,
                isDynamic = isDynamic,
                confidence = ImportConfidence.LOCAL,
                mappedPackage = null,
                status = DependencyStatus.LOCAL_MODULE,
                isAndroidCompatible = true,
                notes = collisionNote
            )
        }

        // 3. Check if standard library module (os, sys, json, math, datetime, asyncio, etc.)
        if (PythonStdLib.isStdLib(rootModule)) {
            return DetectedImport(
                moduleName = rootModule,
                sourceFile = sourceFile,
                lineNumber = lineNum,
                rawStatement = rawStatement,
                submodulesOrClasses = submodulesOrClasses,
                isRelative = false,
                isConditional = isConditional,
                isDynamic = isDynamic,
                confidence = ImportConfidence.BUILTIN,
                mappedPackage = null,
                status = DependencyStatus.BUILTIN,
                isAndroidCompatible = true,
                notes = "Python standard library (Built-in runtime module, no installation needed)"
            )
        }

        // 4. Check known mapping database (requests -> requests, PIL -> Pillow, yaml -> PyYAML, telegram -> python-telegram-bot, etc.)
        val mapping = ImportMappingDatabase.findMapping(rootModule, rawStatement, submodulesOrClasses)
        if (mapping != null) {
            val isAmbiguous = mapping.ambiguousCandidates.isNotEmpty() || mapping.confidence == ImportConfidence.UNKNOWN

            return DetectedImport(
                moduleName = rootModule,
                sourceFile = sourceFile,
                lineNumber = lineNum,
                rawStatement = rawStatement,
                submodulesOrClasses = submodulesOrClasses,
                isRelative = false,
                isConditional = isConditional,
                isDynamic = isDynamic,
                confidence = if (isAmbiguous) ImportConfidence.UNKNOWN else mapping.confidence,
                mappedPackage = if (mapping.packageName.isNotEmpty()) mapping.packageName else null,
                status = DependencyStatus.MISSING,
                candidatePackages = mapping.ambiguousCandidates,
                isAndroidCompatible = mapping.isAndroidCompatible,
                requiresNativeRecipe = mapping.requiresNativeRecipe,
                notes = if (isAmbiguous) {
                    "Multiple packages match '$rootModule'. Please select the specific library used."
                } else {
                    mapping.description
                }
            )
        }

        // 5. Unknown third-party import: do NOT assume import name == pip package name blindly!
        return DetectedImport(
            moduleName = rootModule,
            sourceFile = sourceFile,
            lineNumber = lineNum,
            rawStatement = rawStatement,
            submodulesOrClasses = submodulesOrClasses,
            isRelative = false,
            isConditional = isConditional,
            isDynamic = isDynamic,
            confidence = ImportConfidence.UNKNOWN,
            mappedPackage = null,
            status = DependencyStatus.MISSING,
            candidatePackages = listOf(
                CandidatePackage(rootModule, "Exact name package on PyPI (unverified)", "latest", 0.5f)
            ),
            isAndroidCompatible = true,
            requiresNativeRecipe = false,
            notes = "Unverified import. Search PyPI to confirm providing package before installing."
        )
    }
}
