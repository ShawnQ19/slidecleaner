package com.gallery.cleaner.ui.screen.processed

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gallery.cleaner.domain.repository.MediaRepository
import com.gallery.cleaner.util.log.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class ProcessedMonthViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val TAG = "ProcessedMonthVM"
    }

    private val yearMonthStr: String = savedStateHandle.get<String>("yearMonth") ?: ""

    private val _uiState = MutableStateFlow(ProcessedMonthUiState())
    val uiState: StateFlow<ProcessedMonthUiState> = _uiState.asStateFlow()

    init {
        loadMediaItems()
    }

    private fun loadMediaItems() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                val yearMonth = YearMonth.parse(yearMonthStr)
                AppLogger.enter(TAG, "loadMediaItems", "yearMonth" to yearMonth)

                val items = mediaRepository.getProcessedMediaItemsByMonth(yearMonth)
                AppLogger.dataChange(TAG, "已整理月份加载完成", "yearMonth=$yearMonth, items=${items.size}")
                _uiState.update {
                    it.copy(
                        mediaItems = items,
                        isLoading = false,
                        error = null
                    )
                }

                AppLogger.exit(TAG, "loadMediaItems", "items=${items.size}")
            } catch (e: Exception) {
                AppLogger.exception(TAG, "加载已整理月份异常", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "加载失败"
                    )
                }
            }
        }
    }

    fun retry() {
        loadMediaItems()
    }
}