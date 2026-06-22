package com.gallery.cleaner.ui.screen.media

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gallery.cleaner.data.repository.ProcessedMediaStore
import com.gallery.cleaner.domain.model.DeleteQueue
import com.gallery.cleaner.domain.model.MediaItem
import com.gallery.cleaner.domain.repository.MediaRepository
import com.gallery.cleaner.domain.repository.TrashResult
import com.gallery.cleaner.domain.undo.QueueForDeleteCommand
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
        val maxIndex = currentItems.lastIndex
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
        val removedIndex = currentItems.indexOfFirst { it.id == item.id }
        val oldIndex = _uiState.value.currentIndex
        viewModelScope.launch {
            val command = QueueForDeleteCommand(
                item = item,
                removedIndex = removedIndex,
                oldIndex = oldIndex,
                onQueue = { executeQueueForDelete(it, removedIndex, oldIndex) },
                onRestore = { restoredItem, idx, curIdx -> executeRestoreFromDelete(restoredItem, idx, curIdx) }
            )
            undoManager.execute(command)
        }
    }

    protected open fun executeQueueForDelete(item: MediaItem, removedIndex: Int, oldIndex: Int) {
        _deleteQueueItems.add(item)
        val items = currentItems.toMutableList()
        items.removeAll { it.id == item.id }
        val newIndex = computeNewIndex(items, removedIndex, oldIndex)
        _uiState.update { it.copy(items = items, currentIndex = newIndex, showDeleteDialog = false) }
        updateDeleteQueue()
        if (_deleteQueueItems.size >= DeleteQueue.MAX_QUEUE_SIZE || items.isEmpty()) {
            _uiState.update { it.copy(showDeleteDialog = true) }
        }
    }

    protected open fun executeRestoreFromDelete(item: MediaItem, removedIndex: Int, oldIndex: Int) {
        _deleteQueueItems.remove(item)
        updateDeleteQueue()
        val items = currentItems.toMutableList()
        val insertIndex = removedIndex.coerceIn(0, items.size)
        items.add(insertIndex, item)
        _uiState.update {
            it.copy(
                items = items,
                currentIndex = oldIndex.coerceIn(0, items.lastIndex),
                showDeleteDialog = false,
                deleteSuccess = true,
                deleteMessage = "已撤销加入删除队列"
            )
        }
    }

    private fun computeNewIndex(items: List<MediaItem>, removedIndex: Int, oldIndex: Int): Int {
        return when {
            items.isEmpty() -> 0
            removedIndex < 0 -> oldIndex.coerceAtMost(items.size - 1)
            removedIndex < oldIndex -> (oldIndex - 1).coerceAtLeast(0)
            removedIndex == oldIndex -> oldIndex.coerceAtMost(items.lastIndex)
            else -> oldIndex.coerceAtMost(items.lastIndex)
        }
    }

    open fun keepCurrent(silent: Boolean = false) {
        val item = _uiState.value.currentItem ?: return
        if (_uiState.value.showDeleteDialog) return
        viewModelScope.launch {
            keptItems.add(item)
            AppLogger.userAction(TAG, "keepCurrent", "item.id=${item.id}, keptCount=${keptItems.size}")
        }
    }

    fun unkeepCurrent(item: MediaItem) {
        keptItems.remove(item)
        AppLogger.userAction(TAG, "unkeepCurrent", "item.id=${item.id}, keptCount=${keptItems.size}")
    }

    fun isItemKept(item: MediaItem): Boolean = keptItems.contains(item)

    fun undo() {
        if (_uiState.value.showDeleteDialog) return
        viewModelScope.launch { undoManager.undo() }
    }

    fun redo() {
        if (_uiState.value.showDeleteDialog) return
        viewModelScope.launch { undoManager.redo() }
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
        updateDeleteQueue()
        finalizeKeptItems()
        _uiState.update {
            it.copy(
                showDeleteDialog = false,
                deleteSuccess = true,
                deleteMessage = message
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

    open fun clearDeleteQueue() {
        _deleteQueueItems.clear()
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
