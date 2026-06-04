package com.gallery.cleaner.ui.screen.random

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gallery.cleaner.data.repository.ProcessedMediaStore
import com.gallery.cleaner.domain.model.DeleteQueue
import com.gallery.cleaner.domain.model.MediaItem
import com.gallery.cleaner.domain.repository.MediaRepository
import com.gallery.cleaner.domain.repository.TrashResult
import com.gallery.cleaner.domain.undo.QueueForDeleteCommand
import com.gallery.cleaner.domain.undo.UndoCommand
import com.gallery.cleaner.domain.undo.UndoManager
import com.gallery.cleaner.domain.usecase.DeleteMediaUseCase
import com.gallery.cleaner.util.log.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RandomCleanupViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val deleteMediaUseCase: DeleteMediaUseCase,
    private val processedMediaStore: ProcessedMediaStore
) : ViewModel() {

    companion object {
        private const val TAG = "RandomCleanupVM"
    }

    private val _deleteQueueItems = mutableSetOf<MediaItem>()
    // 左滑保留的项（内存缓存，退出/确认删除时才统一 markProcessed）
    private val keptItems = mutableSetOf<MediaItem>()

    private val _uiState = MutableStateFlow(RandomCleanupUiState())
    val uiState: StateFlow<RandomCleanupUiState> = _uiState.asStateFlow()
    val undoManager = UndoManager(maxHistory = 10)

    init {
        loadBatch()
    }

    fun loadBatch(preserveResultMessage: Boolean = false) {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            try {
                _uiState.update {
                    it.copy(
                        isLoading = true,
                        error = null,
                        deleteMessage = if (preserveResultMessage) it.deleteMessage else "",
                        deleteSuccess = if (preserveResultMessage) it.deleteSuccess else false
                    )
                }
                val processedUris = processedMediaStore.getProcessedUris()
                val items = mediaRepository.getRandomMediaItems(processedUris, DeleteQueue.MAX_QUEUE_SIZE)
                AppLogger.dataChange(TAG, "加载随机批次", "processed=${processedUris.size}, items=${items.size}")
                _deleteQueueItems.clear()
                undoManager.clear()
                _uiState.update {
                    it.copy(
                        batchItems = items,
                        currentIndex = 0,
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
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "随机清理加载失败"
                    )
                }
            }
        }
    }

    fun setCurrentIndex(index: Int) {
        val maxIndex = _uiState.value.batchItems.lastIndex
        val safeIndex = when {
            maxIndex < 0 -> 0
            index < 0 -> 0
            index > maxIndex -> maxIndex
            else -> index
        }
        _uiState.update { it.copy(currentIndex = safeIndex) }
    }

    fun keepCurrent(silent: Boolean = false) {
        val item = _uiState.value.currentItem ?: return
        if (_uiState.value.showDeleteDialog) return
        val oldIndex = _uiState.value.currentIndex

        viewModelScope.launch {
            try {
                val command = object : UndoCommand {
                    override val description: String = "保留 ${item.name}"

                    override suspend fun execute() {
                        executeKeepCurrent(item, oldIndex, silent)
                    }

                    override suspend fun undo() {
                        restoreKeptItem(item, oldIndex, silent)
                    }
                }
                undoManager.execute(command)
            } catch (e: Exception) {
                AppLogger.exception(TAG, "保留处理失败", e)
                _uiState.update {
                    it.copy(deleteMessage = e.message ?: "保留处理失败", deleteSuccess = false)
                }
            }
        }
    }

    fun queueForDelete(item: MediaItem) {
        if (_uiState.value.showDeleteDialog) return
        if (_deleteQueueItems.any { it.uri == item.uri }) return

        val currentItems = _uiState.value.batchItems.toMutableList()
        val removedIndex = currentItems.indexOfFirst { it.uri == item.uri }
        if (removedIndex < 0) return

        val oldIndex = _uiState.value.currentIndex
        viewModelScope.launch {
            try {
                val command = QueueForDeleteCommand(
                    item = item,
                    removedIndex = removedIndex,
                    oldIndex = oldIndex,
                    onQueue = { executeQueueForDelete(it, removedIndex, oldIndex) },
                    onRestore = { restoredItem, index, currentIndex ->
                        restoreFromDelete(restoredItem, index, currentIndex)
                    }
                )
                undoManager.execute(command)
            } catch (e: Exception) {
                AppLogger.exception(TAG, "加入删除队列失败", e)
                _uiState.update {
                    it.copy(deleteMessage = e.message ?: "加入删除队列失败", deleteSuccess = false)
                }
            }
        }
    }

    fun showDeleteConfirmDialog() {
        if (_deleteQueueItems.isNotEmpty()) {
            _uiState.update { it.copy(showDeleteDialog = true) }
        }
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
                is TrashResult.Success -> {
                    AppLogger.i(TAG, "删除成功: ${result.deletedCount} 项")
                    val isTrashSupported = deleteMediaUseCase.repository.isTrashSupported()
                    if (isTrashSupported && result.deletedCount == 0) {
                        AppLogger.d(TAG, "IntentSender 异步处理中，等待系统回调")
                        _uiState.update { it.copy(showDeleteDialog = false) }
                    } else {
                                                finalizeKeptItems()
                        finalizeDeleteQueue(
                            message = if (isTrashSupported) {
                                "已移入系统回收站 ${result.deletedCount} 项"
                            } else {
                                "已永久删除 ${result.deletedCount} 项"
                            }
                        )
                    }
                }

                is TrashResult.Error -> {
                    AppLogger.e(TAG, "删除失败: ${result.message}")
                    _uiState.update {
                        it.copy(
                            showDeleteDialog = false,
                            deleteSuccess = false,
                            deleteMessage = "删除失败: ${result.message}"
                        )
                    }
                }
            }

            AppLogger.exit(TAG, "confirmDelete")
        }
    }

    fun onTrashIntentResult(success: Boolean, count: Int) {
        AppLogger.enter(TAG, "onTrashIntentResult", "success" to success, "count" to count)
        viewModelScope.launch {
            if (success) {
                finalizeKeptItems()
                finalizeDeleteQueue("已移入系统回收站 $count 项")
            } else {
                _uiState.update {
                    it.copy(
                        showDeleteDialog = false,
                        deleteSuccess = false,
                        deleteMessage = if (success) "删除已完成" else "已取消删除"
                    )
                }
            }
        }
        AppLogger.exit(TAG, "onTrashIntentResult")
    }

    fun onBackPressed() {
        AppLogger.enter(TAG, "onBackPressed", Pair("keptCount", keptItems.size), Pair("deleteQueue", _deleteQueueItems.size))
        finalizeKeptItems()
        AppLogger.exit(TAG, "onBackPressed")
    }

    private fun finalizeKeptItems() {
        if (keptItems.isEmpty()) return
        val uris = keptItems.map { it.uri }
        AppLogger.i(TAG, "finalizeKeptItems: 持久化 ${uris.size} 个保留项")
        viewModelScope.launch {
            processedMediaStore.markProcessed(uris)
        }
        keptItems.clear()
    }

    fun dismissDeleteDialog() {
        _uiState.update { it.copy(showDeleteDialog = false) }
    }

    fun dismissResultMessage() {
        _uiState.update { it.copy(deleteSuccess = false, deleteMessage = "") }
    }

    fun refreshBatch() {
        loadBatch()
    }

    fun undo() {
        if (_uiState.value.showDeleteDialog) return
        viewModelScope.launch {
            undoManager.undo()
        }
    }

    private fun executeQueueForDelete(item: MediaItem, removedIndex: Int, oldIndex: Int) {
        _deleteQueueItems.add(item)

        val currentItems = _uiState.value.batchItems.toMutableList()
        currentItems.removeAll { it.uri == item.uri }

        val newIndex = when {
            currentItems.isEmpty() -> 0
            removedIndex < oldIndex -> (oldIndex - 1).coerceAtLeast(0)
            removedIndex == oldIndex -> oldIndex.coerceAtMost(currentItems.lastIndex)
            else -> oldIndex.coerceAtMost(currentItems.lastIndex)
        }

        AppLogger.userAction(TAG, "queueForDelete", "item.id=${item.id}, queueSize=${_deleteQueueItems.size}")

        _uiState.update {
            it.copy(
                batchItems = currentItems,
                currentIndex = newIndex,
                showDeleteDialog = false
            )
        }
        updateDeleteQueue()

        if (_deleteQueueItems.size >= DeleteQueue.MAX_QUEUE_SIZE || currentItems.isEmpty()) {
            _uiState.update { it.copy(showDeleteDialog = true) }
        }
    }

    private fun restoreFromDelete(item: MediaItem, removedIndex: Int, oldIndex: Int) {
        _deleteQueueItems.remove(item)

        val currentItems = _uiState.value.batchItems.toMutableList()
        val insertIndex = removedIndex.coerceIn(0, currentItems.size)
        currentItems.add(insertIndex, item)

        AppLogger.userAction(TAG, "undoDeleteQueue", "item.id=${item.id}, insertIndex=$insertIndex, total=${currentItems.size}")

        _uiState.update {
            it.copy(
                batchItems = currentItems,
                currentIndex = oldIndex.coerceIn(0, currentItems.lastIndex),
                showDeleteDialog = false,
                deleteSuccess = true,
                deleteMessage = "已撤销加入删除队列"
            )
        }
        updateDeleteQueue()
    }

    private suspend fun executeKeepCurrent(item: MediaItem, oldIndex: Int, silent: Boolean) {
        keptItems.add(item)
        AppLogger.userAction(TAG, "keepCurrent", "item.id=${item.id}, keptCount=${keptItems.size}")
    }

    private suspend fun restoreKeptItem(item: MediaItem, oldIndex: Int, silent: Boolean) {
        keptItems.remove(item)
        AppLogger.userAction(TAG, "undoKeepCurrent", "item.id=${item.id}, keptCount=${keptItems.size}")
    }

    /**
     * 从保留集合中移除（用户反悔，上滑删除已保留的照片时调用）
     */
    fun unkeepCurrent(item: MediaItem) {
        keptItems.remove(item)
        AppLogger.userAction(TAG, "unkeepCurrent", "item.id=${item.id}, keptCount=${keptItems.size}")
    }

    fun isItemKept(item: MediaItem): Boolean = keptItems.contains(item)

    private fun updateDeleteQueue() {
        _uiState.update { state ->
            state.copy(
                deleteQueue = DeleteQueue(
                    items = _deleteQueueItems.toList(),
                    currentMonth = null,
                    monthTotalCount = state.deleteQueue.monthTotalCount
                )
            )
        }
    }

    private suspend fun finalizeDeleteQueue(message: String) {

        _deleteQueueItems.clear()
        updateDeleteQueue()
        undoManager.clear()
        _uiState.update {
            it.copy(
                showDeleteDialog = false,
                deleteSuccess = true,
                deleteMessage = message
            )
        }

        loadBatch(preserveResultMessage = true)
    }
}
