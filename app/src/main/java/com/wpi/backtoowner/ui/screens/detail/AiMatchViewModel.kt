package com.wpi.backtoowner.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wpi.backtoowner.domain.usecase.AnalyzeMatchUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AiMatchUiState {
    object Idle : AiMatchUiState()
    object Loading : AiMatchUiState()
    data class Success(val result: String) : AiMatchUiState()
    data class Error(val message: String) : AiMatchUiState()
}

@HiltViewModel
class AiMatchViewModel @Inject constructor(
    private val analyzeMatchUseCase: AnalyzeMatchUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<AiMatchUiState>(AiMatchUiState.Idle)
    val uiState: StateFlow<AiMatchUiState> = _uiState.asStateFlow()

    fun analyzeMatch(lostDescription: String, foundImageUrl: String) {
        viewModelScope.launch {
            _uiState.value = AiMatchUiState.Loading
            try {
                val result = analyzeMatchUseCase(lostDescription, foundImageUrl)
                if (result != null) {
                    _uiState.value = AiMatchUiState.Success(result)
                } else {
                    _uiState.value = AiMatchUiState.Error("Failed to get analysis from Gemini.")
                }
            } catch (e: Exception) {
                _uiState.value = AiMatchUiState.Error(e.message ?: "An unknown error occurred.")
            }
        }
    }

    fun reset() {
        _uiState.value = AiMatchUiState.Idle
    }
}
