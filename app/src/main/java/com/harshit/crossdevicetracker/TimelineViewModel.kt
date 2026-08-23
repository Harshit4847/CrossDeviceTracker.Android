package com.harshit.crossdevicetracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class TimelineViewModel(
    private val repository: TimelineRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(TimelineUiState())
    val uiState: StateFlow<TimelineUiState> = _uiState.asStateFlow()
    
    init {
        loadTimeline()
    }
    
    fun loadTimeline() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            val result = repository.getTimeline()
            result.onSuccess { timeline ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    entries = timeline.entries,
                    error = null
                )
            }
            result.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = error.message
                )
            }
        }
    }
    
    fun refresh() {
        loadTimeline()
    }
    
    private fun formatTime(isoString: String): String {
        return try {
            val instant = Instant.parse(isoString)
            val dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
            val formatter = DateTimeFormatter.ofPattern("HH:mm")
            dateTime.format(formatter)
        } catch (e: Exception) {
            isoString
        }
    }
    
    private fun formatDuration(seconds: Long): String {
        val minutes = seconds / 60
        val hours = minutes / 60
        val remainingMinutes = minutes % 60
        
        return when {
            hours > 0 -> "${hours}h ${remainingMinutes}m"
            remainingMinutes > 0 -> "${remainingMinutes}m"
            else -> "${seconds}s"
        }
    }
}

data class TimelineUiState(
    val isLoading: Boolean = false,
    val entries: List<TimelineEntry> = emptyList(),
    val error: String? = null
)
