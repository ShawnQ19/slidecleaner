package com.gallery.cleaner.ui.screen.preview

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gallery.cleaner.domain.usecase.GetMediaItemUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PreviewViewModel @Inject constructor(
    private val getMediaItemUseCase: GetMediaItemUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val mediaId: Long = savedStateHandle.get<Long>("mediaId") ?: 0L

    private val _uiState = MutableStateFlow(PreviewUiState(isLoading = true))
    val uiState: StateFlow<PreviewUiState> = _uiState.asStateFlow()

    init {
        loadMediaItem()
    }

    private fun loadMediaItem() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                val item = getMediaItemUseCase(mediaId)
                if (item != null) {
                    _uiState.update {
                        it.copy(mediaItem = item, isLoading = false)
                    }
                } else {
                    _uiState.update {
                        it.copy(isLoading = false, error = "媒体文件不存在")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "加载失败")
                }
            }
        }
    }
}
