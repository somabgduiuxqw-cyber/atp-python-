package com.example.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.engine.terminal.TerminalCommandResult
import com.example.engine.terminal.TerminalEngine
import com.example.engine.terminal.TerminalEnvironmentInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class TerminalViewModel(private val context: Context) : ViewModel() {

    private val terminalEngine = TerminalEngine(context.filesDir)

    private val _history = MutableStateFlow<List<TerminalCommandResult>>(emptyList())
    val history: StateFlow<List<TerminalCommandResult>> = _history.asStateFlow()

    private val _isExecuting = MutableStateFlow(false)
    val isExecuting: StateFlow<Boolean> = _isExecuting.asStateFlow()

    private val _envInfo = MutableStateFlow<TerminalEnvironmentInfo?>(null)
    val envInfo: StateFlow<TerminalEnvironmentInfo?> = _envInfo.asStateFlow()

    init {
        loadEnvironment()
    }

    private fun loadEnvironment() {
        _envInfo.value = terminalEngine.getEnvironmentInfo()
    }

    fun executeCommand(command: String) {
        if (command.isBlank() || _isExecuting.value) return
        viewModelScope.launch {
            _isExecuting.value = true
            val result = terminalEngine.execute(command)
            _history.value = _history.value + result
            _isExecuting.value = false
        }
    }

    fun clearHistory() {
        _history.value = emptyList()
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TerminalViewModel(context) as T
        }
    }
}
