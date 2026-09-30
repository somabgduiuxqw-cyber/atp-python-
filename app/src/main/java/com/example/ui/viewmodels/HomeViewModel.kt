package com.example.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.entity.ProjectEntity
import com.example.data.repository.ProjectRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.InputStream

class HomeViewModel(
    private val projectRepository: ProjectRepository
) : ViewModel() {

    val projects: StateFlow<List<ProjectEntity>> = projectRepository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createProjectFromTemplate(
        name: String,
        appName: String,
        packageName: String,
        framework: String,
        templateId: String,
        onCreated: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val proj = projectRepository.createProject(name, appName, packageName, framework, templateId)
            onCreated(proj.id)
        }
    }

    fun importSingleFile(
        name: String,
        appName: String,
        packageName: String,
        framework: String,
        fileName: String,
        content: String,
        onCreated: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val proj = projectRepository.importSingleFile(name, appName, packageName, framework, fileName, content)
            onCreated(proj.id)
        }
    }

    fun importZip(
        name: String,
        appName: String,
        packageName: String,
        framework: String,
        zipStream: InputStream,
        onSuccess: (Long) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val res = projectRepository.importZipProject(name, appName, packageName, framework, zipStream)
            if (res.isSuccess) {
                onSuccess(res.getOrThrow().id)
            } else {
                onError(res.exceptionOrNull()?.message ?: "Failed to import ZIP archive")
            }
        }
    }

    fun deleteProject(project: ProjectEntity) {
        viewModelScope.launch {
            projectRepository.deleteProject(project)
        }
    }

    class Factory(private val repository: ProjectRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(repository) as T
        }
    }
}
