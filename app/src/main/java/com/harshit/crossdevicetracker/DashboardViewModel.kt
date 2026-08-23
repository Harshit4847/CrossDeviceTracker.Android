package com.example

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val repository: DashboardRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    
    init {
        loadDashboard()
    }
    
    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val result = repository.getDashboardSummary()
            result.onSuccess { summary ->
                Log.d("DashboardDebug", "ScreenTime: ${summary.today.totalScreenTimeSeconds}")
                Log.d("DashboardDebug", "SessionCount: ${summary.today.sessionCount}")
                val formattedTime = formatScreenTime(summary.today.totalScreenTimeSeconds)
                val app = summary.mostUsedApp

                val formattedMostUsedAppTime =
                    app?.let { formatScreenTime(it.durationSeconds) } ?: "--"

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    screenTime = formattedTime,
                    deviceCount = summary.deviceCount,
                    appCount = summary.appCount,
                    sessionCount = summary.today.sessionCount,
                    mostUsedApp = app?.appName ?: "--",
                    mostUsedAppTime = formattedMostUsedAppTime,
                    error = null
                )
                Log.d("DashboardDebug", "UI State: ${_uiState.value}")
            }
            result.onFailure { error ->
                Log.e("DashboardDebug", "ViewModel Error", error)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = error.message
                )
            }
        }
        loadActiveDeviceCount()
    }

    private fun loadActiveDeviceCount() {
        viewModelScope.launch {
            val result = repository.getActiveDeviceCount()
            result.onSuccess { count ->
                _uiState.value = _uiState.value.copy(activeDeviceCount = count)
            }
            result.onFailure { error ->
                Log.e("DashboardDebug", "Active Device Count Error", error)
            }
        }
    }
    
    fun refresh() {
        loadDashboard()
    }
    
    private fun formatScreenTime(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        return if (hours > 0) {
            "${hours}h ${minutes}m"
        } else {
            "${minutes}m"
        }
    }
}

data class DashboardUiState(
    val isLoading: Boolean = false,
    val screenTime: String = "--",
    val deviceCount: Int = 0,
    val activeDeviceCount: Int = 0,
    val appCount: Int = 0,
    val sessionCount: Int = 0,
    val mostUsedApp: String = "--",
    val mostUsedAppTime: String = "--",
    val error: String? = null
)
