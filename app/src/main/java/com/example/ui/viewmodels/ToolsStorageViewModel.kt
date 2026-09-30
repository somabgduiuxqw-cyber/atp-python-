package com.example.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.DeviceResourceReport
import com.example.data.model.ToolStatus
import com.example.data.repository.PackageRepository
import com.example.engine.build.EnvironmentDetector
import com.example.engine.storage.WorkspaceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ToolsStorageViewModel(
    private val environmentDetector: EnvironmentDetector,
    private val workspaceManager: WorkspaceManager,
    private val packageRepository: PackageRepository
) : ViewModel() {

    private val _tools = MutableStateFlow<List<ToolStatus>>(emptyList())
    val tools: StateFlow<List<ToolStatus>> = _tools.asStateFlow()

    private val _isLoadingTools = MutableStateFlow(false)
    val isLoadingTools: StateFlow<Boolean> = _isLoadingTools.asStateFlow()

    private val _deviceReport = MutableStateFlow<DeviceResourceReport?>(null)
    val deviceReport: StateFlow<DeviceResourceReport?> = _deviceReport.asStateFlow()

    private val _storageBreakdown = MutableStateFlow<Map<String, Long>>(emptyMap())
    val storageBreakdown: StateFlow<Map<String, Long>> = _storageBreakdown.asStateFlow()

    init {
        refreshAll()
    }

    fun refreshAll() {
        viewModelScope.launch {
            _isLoadingTools.value = true
            _tools.value = environmentDetector.inspectTools()
            _deviceReport.value = environmentDetector.checkDeviceResources()
            _storageBreakdown.value = workspaceManager.getStorageBreakdown()
            _isLoadingTools.value = false
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            packageRepository.clearPackageCache()
            _storageBreakdown.value = workspaceManager.getStorageBreakdown()
        }
    }

    class Factory(
        private val environmentDetector: EnvironmentDetector,
        private val workspaceManager: WorkspaceManager,
        private val packageRepository: PackageRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ToolsStorageViewModel(environmentDetector, workspaceManager, packageRepository) as T
        }
    }
}
