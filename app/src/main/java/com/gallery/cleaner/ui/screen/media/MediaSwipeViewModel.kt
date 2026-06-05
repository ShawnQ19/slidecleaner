package com.gallery.cleaner.ui.screen.media

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.gallery.cleaner.data.repository.ProcessedMediaStore
import com.gallery.cleaner.domain.model.DeleteQueue
import com.gallery.cleaner.domain.repository.MediaRepository
import com.gallery.cleaner.domain.usecase.DeleteMediaUseCase
import com.gallery.cleaner.util.log.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class MediaSwipeViewModel @Inject constructor(
    mediaRepository: MediaRepository,
    deleteMediaUseCase: DeleteMediaUseCase,
    processedMediaStore: ProcessedMediaStore,
    savedStateHandle: SavedStateHandle
) : CleanupViewModel(mediaRepository, deleteMediaUseCase, processedMediaStore) {

    companion object {
        private const val TAG = "MediaSwipeVM"
    }

    private val yearMonthStr: String = savedStateHandle.get<String>("yearMonth") ?: ""

    override val uiState: StateFlow<CleanupUiState> = _uiState

    init {
        loadMediaItems()
    }

    private fun loadMediaItems() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                val yearMonth = YearMonth.parse(yearMonthStr)
                AppLogger.enter(TAG, "loadMediaItems", "yearMonth" to yearMonth)

                val items = mediaRepository.getMediaItemsByMonth(yearMonth)
                AppLogger.dataChange(TAG, "月份媒体加载完成", "yearMonth=$yearMonth, items=${items.size}")
                _uiState.update {
                    it.copy(
                        items = items,
                        deleteQueue = DeleteQueue(
                            items = _deleteQueueItems.toList(),
                            currentMonth = yearMonth,
                            monthTotalCount = items.size
                        )
                    )
                }

                AppLogger.exit(TAG, "loadMediaItems", "items=${items.size}")
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                AppLogger.exception(TAG, "加载媒体列表异常", e)
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "加载失败")
                }
            }
        }
    }

    override fun showDeleteConfirmDialog() {
        AppLogger.d(TAG, "显示删除确认对话框")
        _uiState.update { it.copy(showDeleteDialog = true) }
    }
}