package com.gallery.cleaner.ui.screen.random

import androidx.lifecycle.viewModelScope
import com.gallery.cleaner.data.repository.ProcessedMediaStore
import com.gallery.cleaner.domain.model.DeleteQueue
import com.gallery.cleaner.domain.repository.MediaRepository
import com.gallery.cleaner.domain.usecase.DeleteMediaUseCase
import com.gallery.cleaner.ui.screen.media.CleanupUiState
import com.gallery.cleaner.ui.screen.media.CleanupViewModel
import com.gallery.cleaner.util.log.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RandomCleanupViewModel @Inject constructor(
    mediaRepository: MediaRepository,
    deleteMediaUseCase: DeleteMediaUseCase,
    processedMediaStore: ProcessedMediaStore
) : CleanupViewModel(mediaRepository, deleteMediaUseCase, processedMediaStore) {

    companion object {
        private const val TAG = "RandomCleanupVM"
    }

    override val uiState: StateFlow<CleanupUiState> = _uiState

    init {
        loadBatch()
    }

    fun loadBatch() {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                val processedUris = processedMediaStore.getProcessedUris()
                val items = mediaRepository.getRandomMediaItems(processedUris, DeleteQueue.MAX_QUEUE_SIZE)
                AppLogger.dataChange(TAG, "加载随机批次", "processed=${processedUris.size}, items=${items.size}")
                _deleteQueueItems.clear()
                keptItems.clear()
                undoManager.clear()
                _uiState.update {
                    it.copy(
                        items = items,
                        currentIndex = 0,
                        batchTotal = items.size,
                        deleteQueue = DeleteQueue(
                            items = emptyList(),
                            currentMonth = null,
                            monthTotalCount = items.size
                        ),
                        processedCount = processedUris.size,
                        isLoading = false,
                        error = null,
                        showDeleteDialog = false,
                        deleteSuccess = false,
                        deleteMessage = ""
                    )
                }
            } catch (e: Exception) {
                AppLogger.exception(TAG, "加载随机批次失败", e)
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "随机清理加载失败")
                }
            }
        }
    }

    override fun handleDeleteSuccess(result: com.gallery.cleaner.domain.repository.TrashResult.Success) {
        AppLogger.i(TAG, "删除成功: ${result.deletedCount} 项")
        finalizeKeptItems()
        finalizeDeleteQueue(
            message = if (deleteMediaUseCase.repository.isTrashSupported()) {
                "已移入系统回收站 ${result.deletedCount} 项"
            } else {
                "已永久删除 ${result.deletedCount} 项"
            }
        )
    }

    fun loadNextBatch() {
        loadBatch()
    }

    fun refreshBatch() {
        loadBatch()
    }
}
