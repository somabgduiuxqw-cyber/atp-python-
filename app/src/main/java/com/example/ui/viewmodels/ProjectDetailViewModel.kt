package com.example.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.entity.ProjectEntity
import com.example.data.model.*
import com.example.data.repository.BuildRepository
import com.example.data.repository.PackageRepository
import com.example.data.repository.ProjectRepository
import com.example.engine.analyzer.DependencyResolver
import com.example.engine.analyzer.ImportMappingDatabase
import com.example.engine.build.EnvironmentDetector
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.io.File

enum class ProjectTab {
    EDITOR,
    ANALYZER,
    PACKAGES,
    CONFIG,
    BUILD
}

class ProjectDetailViewModel(
    private val context: Context,
    private val projectId: Long,
    private val projectRepository: ProjectRepository,
    private val packageRepository: PackageRepository,
    private val buildRepository: BuildRepository,
    private val environmentDetector: EnvironmentDetector
) : ViewModel() {

    private val _project = MutableStateFlow<ProjectEntity?>(null)
    val project: StateFlow<ProjectEntity?> = _project.asStateFlow()

    private val _currentTab = MutableStateFlow(ProjectTab.EDITOR)
    val currentTab: StateFlow<ProjectTab> = _currentTab.asStateFlow()

    // Editor state
    private val _editorCode = MutableStateFlow("")
    val editorCode: StateFlow<String> = _editorCode.asStateFlow()

    private val _selectedFile = MutableStateFlow("main.py")
    val selectedFile: StateFlow<String> = _selectedFile.asStateFlow()

    private val _availableFiles = MutableStateFlow<List<String>>(listOf("main.py", "requirements.txt"))
    val availableFiles: StateFlow<List<String>> = _availableFiles.asStateFlow()

    private val _isEditorDirty = MutableStateFlow(false)
    val isEditorDirty: StateFlow<Boolean> = _isEditorDirty.asStateFlow()

    // Analysis state
    private val _analysis = MutableStateFlow<DependencyResolution?>(null)
    val analysis: StateFlow<DependencyResolution?> = _analysis.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _estimatedApkSize = MutableStateFlow(EstimatedApkSize())
    val estimatedApkSize: StateFlow<EstimatedApkSize> = _estimatedApkSize.asStateFlow()

    // PyPI & Packages state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<PyPiPackageInfo>>(emptyList())
    val searchResults: StateFlow<List<PyPiPackageInfo>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _installProgress = MutableStateFlow<PackageInstallProgress?>(null)
    val installProgress: StateFlow<PackageInstallProgress?> = _installProgress.asStateFlow()

    private val _dependencyTree = MutableStateFlow<List<DependencyTreeNode>>(emptyList())
    val dependencyTree: StateFlow<List<DependencyTreeNode>> = _dependencyTree.asStateFlow()

    private val _installedPackages = MutableStateFlow<List<String>>(emptyList())
    val installedPackages: StateFlow<List<String>> = _installedPackages.asStateFlow()

    // Build state
    private val _buildMode = MutableStateFlow(BuildMode.LOCAL)
    val buildMode: StateFlow<BuildMode> = _buildMode.asStateFlow()

    private val _isBuilding = MutableStateFlow(false)
    val isBuilding: StateFlow<Boolean> = _isBuilding.asStateFlow()

    private val _buildSteps = MutableStateFlow<List<BuildStep>>(emptyList())
    val buildSteps: StateFlow<List<BuildStep>> = _buildSteps.asStateFlow()

    private val _buildLogs = MutableStateFlow<List<String>>(emptyList())
    val buildLogs: StateFlow<List<String>> = _buildLogs.asStateFlow()

    private val _apkVerification = MutableStateFlow<ApkVerificationResult?>(null)
    val apkVerification: StateFlow<ApkVerificationResult?> = _apkVerification.asStateFlow()

    private val _onlineJob = MutableStateFlow<OnlineBuildJob?>(null)
    val onlineJob: StateFlow<OnlineBuildJob?> = _onlineJob.asStateFlow()

    private var activeBuildJob: Job? = null

    init {
        loadProject()
    }

    private fun loadProject() {
        viewModelScope.launch {
            val proj = projectRepository.getProjectById(projectId)
            _project.value = proj
            if (proj != null) {
                refreshFilesList(File(proj.directoryPath))
                loadFileContent("main.py")
                refreshInstalledPackages()
                analyzeDependencies()
            }
        }
    }

    fun setTab(tab: ProjectTab) {
        _currentTab.value = tab
        if (tab == ProjectTab.ANALYZER && _analysis.value == null) {
            analyzeDependencies()
        }
    }

    fun selectFile(filename: String) {
        if (_isEditorDirty.value) {
            saveCurrentFile()
        }
        _selectedFile.value = filename
        loadFileContent(filename)
    }

    private fun refreshFilesList(dir: File) {
        val files = dir.walkTopDown()
            .filter { it.isFile && (it.extension == "py" || it.name == "requirements.txt" || it.extension == "json" || it.extension == "txt") }
            .map { it.relativeTo(dir).path }
            .toList()
        _availableFiles.value = if (files.isNotEmpty()) files else listOf("main.py", "requirements.txt")
    }

    private fun loadFileContent(relativePath: String) {
        val proj = _project.value ?: return
        val target = File(proj.directoryPath, relativePath)
        _editorCode.value = if (target.exists()) target.readText() else ""
        _isEditorDirty.value = false
    }

    fun updateEditorCode(newCode: String) {
        _editorCode.value = newCode
        _isEditorDirty.value = true
    }

    fun saveCurrentFile() {
        val proj = _project.value ?: return
        val target = File(proj.directoryPath, _selectedFile.value)
        target.parentFile?.mkdirs()
        target.writeText(_editorCode.value)
        _isEditorDirty.value = false
        // Refresh installed and files list
        refreshFilesList(File(proj.directoryPath))
        // Re-analyze dependencies if python file was saved
        if (_selectedFile.value.endsWith(".py") || _selectedFile.value == "requirements.txt") {
            analyzeDependencies()
        }
    }

    fun createNewFileInProject(fileName: String) {
        val proj = _project.value ?: return
        val file = File(proj.directoryPath, fileName)
        if (!file.exists()) {
            file.parentFile?.mkdirs()
            file.writeText("# $fileName\n")
            refreshFilesList(File(proj.directoryPath))
            selectFile(fileName)
        }
    }

    fun analyzeDependencies() {
        val proj = _project.value ?: return
        viewModelScope.launch {
            _isAnalyzing.value = true
            val projectDir = File(proj.directoryPath)
            refreshInstalledPackages()
            val resolver = DependencyResolver(projectDir, _installedPackages.value.toSet())
            val res = resolver.resolve()
            _analysis.value = res

            // Calculate estimated size
            val confirmedPkgNames = res.required.mapNotNull { it.mappedPackage }
            val fw = try { FrameworkType.valueOf(proj.framework) } catch (e: Exception) { FrameworkType.AUTO }
            _estimatedApkSize.value = resolver.calculateEstimatedApkSize(fw, confirmedPkgNames)

            _isAnalyzing.value = false
        }
    }

    private fun refreshInstalledPackages() {
        val proj = _project.value ?: return
        val projectDir = File(proj.directoryPath)
        val sitePackages = File(projectDir, "site-packages")
        val pkgs = mutableListOf<String>()
        if (sitePackages.exists()) {
            sitePackages.listFiles()?.filter { it.isDirectory }?.forEach {
                pkgs.add(it.name)
            }
        }
        val reqFile = File(projectDir, "requirements.txt")
        if (reqFile.exists()) {
            reqFile.readLines().forEach { line ->
                val name = line.trim().split("=").first().split(">").first().split("<").first().trim()
                if (name.isNotEmpty() && !name.startsWith("#") && !pkgs.contains(name)) {
                    pkgs.add(name)
                }
            }
        }
        _installedPackages.value = pkgs
    }

    fun installSinglePackage(packageName: String) {
        val proj = _project.value ?: return
        viewModelScope.launch {
            val projectDir = File(proj.directoryPath)
            packageRepository.installPackageToProject(projectDir, packageName) { progress ->
                _installProgress.value = progress
            }
            refreshInstalledPackages()
            analyzeDependencies()
            updateDependencyTree()
            delay(1000)
            _installProgress.value = null
        }
    }

    fun installAllConfirmed() {
        val resolution = _analysis.value ?: return
        val toInstall = resolution.required.mapNotNull { it.mappedPackage }.distinct()
        if (toInstall.isEmpty()) return

        viewModelScope.launch {
            val proj = _project.value ?: return@launch
            val projectDir = File(proj.directoryPath)
            for (pkg in toInstall) {
                packageRepository.installPackageToProject(projectDir, pkg) { progress ->
                    _installProgress.value = progress
                }
            }
            refreshInstalledPackages()
            analyzeDependencies()
            updateDependencyTree()
            delay(1000)
            _installProgress.value = null
        }
    }

    fun removeUnusedDependency(pkgName: String) {
        val proj = _project.value ?: return
        viewModelScope.launch {
            val projectDir = File(proj.directoryPath)
            packageRepository.uninstallPackageFromProject(projectDir, pkgName)
            refreshInstalledPackages()
            analyzeDependencies()
        }
    }

    fun resolveAmbiguousPackage(importName: String, selectedPackageName: String) {
        ImportMappingDatabase.setUserCorrection(importName, selectedPackageName)
        viewModelScope.launch {
            packageRepository.recordUserCorrection(importName, selectedPackageName)
            analyzeDependencies()
        }
    }

    fun syncRequirementsTxt() {
        val proj = _project.value ?: return
        val resolution = _analysis.value ?: return
        viewModelScope.launch {
            val projectDir = File(proj.directoryPath)
            val reqFile = File(projectDir, "requirements.txt")
            val currentContent = if (reqFile.exists()) reqFile.readText() else ""
            val confirmedToAdd = resolution.required.mapNotNull { it.mappedPackage }
            val unusedToRemove = resolution.unusedInRequirements.map { it.packageName }

            val resolver = DependencyResolver(projectDir)
            val synced = resolver.generateSyncedRequirements(currentContent, confirmedToAdd, unusedToRemove)
            reqFile.writeText(synced)
            loadFileContent(_selectedFile.value)
            analyzeDependencies()
        }
    }

    fun searchPyPi(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _isSearching.value = true
            _searchResults.value = packageRepository.searchPyPi(query)
            _isSearching.value = false
        }
    }

    fun updateDependencyTree() {
        viewModelScope.launch {
            _dependencyTree.value = packageRepository.buildDependencyTree(_installedPackages.value)
        }
    }

    // Config updates
    fun updateConfig(
        appName: String,
        packageName: String,
        versionName: String,
        versionCode: Int,
        orientation: String,
        framework: String,
        permissions: List<String>,
        backgroundMode: String,
        creatorName: String,
        creatorUsername: String,
        showBranding: Boolean,
        showCreator: Boolean
    ) {
        val current = _project.value ?: return
        val updated = current.copy(
            appName = appName.trim(),
            packageName = packageName.trim(),
            versionName = versionName.trim(),
            versionCode = versionCode,
            orientation = orientation,
            framework = framework,
            permissionsJson = JSONArray(permissions).toString(),
            backgroundMode = backgroundMode,
            creatorName = creatorName.trim(),
            creatorUsername = creatorUsername.trim(),
            showBranding = showBranding,
            showCreator = showCreator
        )
        viewModelScope.launch {
            projectRepository.updateProject(updated)
            _project.value = updated
        }
    }

    fun setBuildMode(mode: BuildMode) {
        _buildMode.value = mode
    }

    fun startBuild() {
        val proj = _project.value ?: return
        if (_isBuilding.value) return

        _isBuilding.value = true
        _apkVerification.value = null
        _buildLogs.value = emptyList()

        val steps = listOf(
            BuildStep("analyze", "Analyzing project", "Validating project workspace and configuration"),
            BuildStep("deps", "Resolving dependencies", "Checking confirmed packages and compatibility"),
            BuildStep("runtime", "Preparing Python runtime", "Packaging Python bytecode and assets"),
            BuildStep("config", "Configuring Android project", "Generating AndroidManifest, permissions, and icons"),
            BuildStep("package", "Packaging APK", "Creating APK ZIP archive with DEX bytecode and assets"),
            BuildStep("sign", "Signing APK", "Generating cryptographic signatures and META-INF block"),
            BuildStep("verify", "Verifying APK", "Deep inspection of APK structure, manifest, and signatures")
        )
        _buildSteps.value = steps

        activeBuildJob = viewModelScope.launch {
            if (_buildMode.value == BuildMode.LOCAL) {
                val confirmed = _analysis.value?.required?.mapNotNull { it.mappedPackage } ?: emptyList()
                val verification = buildRepository.runLocalBuild(proj, confirmed) { stepId, title, log, state ->
                    _buildLogs.value = _buildLogs.value + "[$title] $log"
                    _buildSteps.value = _buildSteps.value.map { s ->
                        if (s.id == stepId) s.copy(state = state, logOutput = log) else s
                    }
                }
                _apkVerification.value = verification
                _isBuilding.value = false
            } else {
                // Online build flow
                _buildLogs.value = listOf("[Upload] Submitting project archive to online build server...")
                val submitRes = buildRepository.submitOnlineBuild(proj)
                if (submitRes.isFailure) {
                    _buildLogs.value = _buildLogs.value + "[Error] Failed to connect: ${submitRes.exceptionOrNull()?.message}"
                    _isBuilding.value = false
                    return@launch
                }

                var job = submitRes.getOrThrow()
                _onlineJob.value = job
                _buildLogs.value = _buildLogs.value + "[Queued] Build ID: ${job.buildId}, Queue pos: ${job.queuePosition ?: 1}"

                // Poll status
                var pollCount = 0
                while (pollCount < 60 && (job.status == "QUEUED" || job.status == "BUILDING")) {
                    delay(3000)
                    val statusRes = buildRepository.getOnlineBuildStatus(job.buildId)
                    if (statusRes.isSuccess) {
                        job = statusRes.getOrThrow()
                        _onlineJob.value = job
                        if (job.logs.isNotEmpty()) {
                            _buildLogs.value = job.logs
                        }
                    }
                    pollCount++
                }

                if (job.status == "COMPLETED" && job.downloadUrl != null) {
                    _buildLogs.value = _buildLogs.value + "[Download] Downloading compiled APK from server..."
                    val dlRes = buildRepository.downloadBuiltApk(job.downloadUrl!!, proj.appName)
                    if (dlRes.isSuccess) {
                        val downloadedApk = dlRes.getOrThrow()
                        // Run verification on downloaded APK!
                        val localBuilder = com.example.engine.build.LocalApkBuilder(context, proj, emptyList()) { _, _, _, _ -> }
                        val verification = localBuilder.verifyApk(downloadedApk, proj)
                        _apkVerification.value = verification
                    }
                }
                _isBuilding.value = false
            }
        }
    }

    fun cancelBuild() {
        activeBuildJob?.cancel()
        _isBuilding.value = false
        val job = _onlineJob.value
        if (job != null) {
            viewModelScope.launch {
                buildRepository.cancelOnlineBuild(job.buildId)
            }
        }
        _buildLogs.value = _buildLogs.value + "[Cancelled] Build process terminated by user."
    }

    class Factory(
        private val context: Context,
        private val projectId: Long,
        private val projectRepository: ProjectRepository,
        private val packageRepository: PackageRepository,
        private val buildRepository: BuildRepository,
        private val environmentDetector: EnvironmentDetector
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProjectDetailViewModel(
                context, projectId, projectRepository, packageRepository, buildRepository, environmentDetector
            ) as T
        }
    }
}
