package com.example.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.entity.BuildHistoryEntity
import com.example.data.repository.BuildRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BuildHistoryViewModel(
    private val buildRepository: BuildRepository
) : ViewModel() {

    val builds: StateFlow<List<BuildHistoryEntity>> = buildRepository.allBuilds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteBuild(id: Long) {
        viewModelScope.launch {
            buildRepository.deleteBuild(id)
        }
    }

    class Factory(private val buildRepository: BuildRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BuildHistoryViewModel(buildRepository) as T
        }
    }
}
