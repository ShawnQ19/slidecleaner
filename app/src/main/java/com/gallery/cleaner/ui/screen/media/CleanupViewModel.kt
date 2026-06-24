package com.gallery.cleaner.ui.screen.media

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gallery.cleaner.data.repository.ProcessedMediaStore
import com.gallery.cleaner.domain.model.DeleteQueue
import com.gallery.cleaner.domain.model.MediaItem
import com.gallery.cleaner.domain.repository.MediaRepository
import com.gallery.cleaner.domain.repository.TrashResult
import com.gallery.cleaner.domain.undo.UndoManager
import com.gallery.cleaner.domain.usecase.DeleteMediaUseCase
import com.gallery.cleaner.util.log.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

abstract class CleanupViewModel(
    protected val mediaRepository: MediaRepository,
    protected val deleteMediaUseCase: DeleteMediaUseCase,
    protected val processedMediaStore: ProcessedMediaStore
) : ViewModel() {

    companion object {
        protected const val TAG = "CleanupVM"
    }

    protected val _uiState = MutableStateFlow(CleanupUiState())
    abstract val uiState: StateFlow<CleanupUiState>

    protected val _deleteQueueItems = ConcurrentHashMap.newKeySet<MediaItem>()
    protected val keptItems = ConcurrentHashMap.newKeySet<MediaItem>()
    val undoManager = UndoManager(maxHistory = 10)

    protected val currentItems: List<MediaItem>
        get() = _uiState.value.items

    open fun setCurrentIndex(index: Int) {
        val maxIndex = _uiState.value.visibleItems.lastIndex
        val safeIndex = when {
            maxIndex < 0 -> 0
            index < 0 -> 0
            index > maxIndex -> maxIndex
            else -> index
        }
        _uiState.update { it.copy(currentIndex = safeIndex) }
    }

    fun queueForDelete(item: MediaItem) {
        AppLogger.userAction(TAG, "queueForDelete", "item.id=${item.id}, queueSize=${_deleteQueueItems.size}")
        if (_deleteQueueItems.contains(item)) {
            AppLogger.w(TAG, "item.id=${item.id} 已在删除队列中，跳过")
            return
        }
        _deleteQueueItems.add(item)
        keptItems.remove(item)
        _uiState.update { state ->
            state.copy(
                hiddenItemIds = state.hiddenItemIds + item.id,
                showDeleteDialog = false
            )
        }
        updateDeleteQueue()
        if (_deleteQueueItems.size >= DeleteQueue.MAX_QUEUE_SIZE) {
            _uiState.update { it.copy(showDeleteDialog = true) }
        }
    }

    fun keepCurrent(silent: Boolean = false) {
        val item = _uiState.value.currentItem ?: return
        if (_uiState.value.showDeleteDialog) return
        keptItems.add(item)
        AppLogger.userAction(TAG, "keepCurrent", "item.id=${item.id}, keptCount=${keptItems.size}")
    }

    fun unkeepCurrent(item: MediaItem) {
        keptItems.remove(item)
        AppLogger.userAction(TAG, "unkeepCurrent", "item.id=${item.id}, keptCount=${keptItems.size}")
    }

    fun isItemKept(item: MediaItem): Boolean = keptItems.contains(item)

    fun undo() {
        if (_uiState.value.showDeleteDialog) return
        val lastDeleted = _deleteQueueItems.lastOrNull() ?: return
        _deleteQueueItems.remove(lastDeleted)
        keptItems.remove(lastDeleted)
        _uiState.update { state ->
            state.copy(
                hiddenItemIds = state.hiddenItemIds - lastDeleted.id,
                showDeleteDialog = false,
                deleteSuccess = true,
                deleteMessage = "已撤销加入删除队列"
            )
        }
        updateDeleteQueue()
    }

    open fun showDeleteConfirmDialog() {
        AppLogger.d(TAG, "显示删除确认对话框")
        _uiState.update { it.copy(showDeleteDialog = true) }
    }

    fun confirmDelete() {
        viewModelScope.launch {
            val itemsToDelete = _deleteQueueItems.toList()
            if (itemsToDelete.isEmpty()) {
                finalizeKeptItems()
                _uiState.update { it.copy(showDeleteDialog = false) }
                return@launch
            }
            AppLogger.enter(TAG, "confirmDelete", "count" to itemsToDelete.size)
            val uris = itemsToDelete.map { it.uri }
            val result = deleteMediaUseCase(uris)
            when (result) {
                is TrashResult.Success -> handleDeleteSuccess(result)
                is TrashResult.Error -> handleDeleteError(result)
            }
            AppLogger.exit(TAG, "confirmDelete")
        }
    }

    protected open fun handleDeleteSuccess(result: TrashResult.Success) {
        AppLogger.i(TAG, "删除成功: ${result.deletedCount} 项")
        val message = if (deleteMediaUseCase.repository.isTrashSupported()) {
            "已移入系统回收站 ${result.deletedCount} 项"
        } else {
            "已永久删除 ${result.deletedCount} 项"
        }
        _deleteQueueItems.clear()
        undoManager.clear()
        _uiState.update { it.copy(hiddenItemIds = emptySet()) }
        updateDeleteQueue()
        finalizeKeptItems()
        _uiState.update {
            it.copy(
                showDeleteDialog = false,
                deleteSuccess = true,
                deleteMessage = message,
                showPostDeleteDialog = true
            )
        }
        onAfterDeleteSuccess()
    }

    protected open fun handleDeleteError(result: TrashResult.Error) {
        AppLogger.e(TAG, "删除失败: ${result.message}")
        _uiState.update {
            it.copy(
                showDeleteDialog = false,
                deleteSuccess = false,
                deleteMessage = "删除失败: ${result.message}"
            )
        }
    }

    protected open fun onAfterDeleteSuccess() {}

    protected fun finalizeDeleteQueue(message: String) {
        _uiState.update {
            it.copy(
                showDeleteDialog = false,
                deleteSuccess = true,
                deleteMessage = message
            )
        }
        onAfterDeleteSuccess()
    }

    fun onBackPressed() {
        AppLogger.enter(TAG, "onBackPressed", "keptCount" to keptItems.size, "deleteQueue" to _deleteQueueItems.size)
        finalizeKeptItems()
        AppLogger.exit(TAG, "onBackPressed")
    }

    protected fun finalizeKeptItems() {
        if (keptItems.isEmpty()) return
        val uris = keptItems.map { it.uri }
        AppLogger.i(TAG, "finalizeKeptItems: 持久化 ${uris.size} 个保留项")
        viewModelScope.launch { processedMediaStore.markProcessed(uris) }
        keptItems.clear()
    }

    fun onTrashIntentResult(success: Boolean, count: Int) {
        AppLogger.enter(TAG, "onTrashIntentResult", "success" to success, "count" to count)
        if (success) {
            finalizeKeptItems()
            finalizeDeleteQueue("已移入系统回收站 $count 项")
        } else {
            _uiState.update {
                it.copy(
                    showDeleteDialog = false,
                    deleteSuccess = false,
                    deleteMessage = "用户取消了删除操作"
                )
            }
        }
        AppLogger.exit(TAG, "onTrashIntentResult")
    }

    fun dismissDeleteDialog() {
        _uiState.update { it.copy(showDeleteDialog = false) }
    }

    fun dismissResultMessage() {
        _uiState.update { it.copy(deleteSuccess = false, deleteMessage = "") }
    }

    fun dismissPostDeleteDialog() {
        _uiState.update { it.copy(showPostDeleteDialog = false) }
    }

    fun showExitConfirmDialog() {
        _uiState.update { it.copy(showExitConfirmDialog = true) }
    }

    fun dismissExitConfirmDialog() {
        _uiState.update { it.copy(showExitConfirmDialog = false) }
    }

    open fun clearDeleteQueue() {
        _deleteQueueItems.clear()
        _uiState.update { it.copy(hiddenItemIds = emptySet()) }
        updateDeleteQueue()
    }

    protected fun updateDeleteQueue() {
        _uiState.update { state ->
            state.copy(
                deleteQueue = DeleteQueue(
                    items = _deleteQueueItems.toList(),
                    currentMonth = state.deleteQueue.currentMonth,
                    monthTotalCount = state.deleteQueue.monthTotalCount
                )
            )
        }
    }
}
