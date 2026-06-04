package com.gallery.cleaner.ui.screen.media

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gallery.cleaner.data.repository.ProcessedMediaStore
import com.gallery.cleaner.domain.model.DeleteQueue
import com.gallery.cleaner.domain.model.MediaItem
import com.gallery.cleaner.domain.repository.MediaRepository
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
import java.time.YearMonth
import javax.inject.Inject
import android.net.Uri

@HiltViewModel
class MediaSwipeViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val deleteMediaUseCase: DeleteMediaUseCase,
    private val processedMediaStore: ProcessedMediaStore,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val TAG = "MediaSwipeVM"
    }

    private val yearMonthStr: String = savedStateHandle.get<String>("yearMonth") ?: ""

    private val _uiState = MutableStateFlow(MediaSwipeUiState())
    val uiState: StateFlow<MediaSwipeUiState> = _uiState.asStateFlow()

    private val _deleteQueueItems = mutableSetOf<MediaItem>()

    // 左滑保留的项（内存缓存，退出/确认删除时才统一 markProcessed）
    private val keptItems = mutableSetOf<MediaItem>()

    val undoManager = UndoManager(maxHistory = 10)

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
                        mediaItems = items,
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
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "加载失败"
                    )
                }
            }
        }
    }

    fun setCurrentIndex(index: Int) {
        _uiState.update { it.copy(currentIndex = index) }
    }

    fun queueForDelete(item: MediaItem) {
        AppLogger.userAction(TAG, "queueForDelete", "item.id=${item.id}, queueSize=${_deleteQueueItems.size}")
        if (_deleteQueueItems.contains(item)) {
            AppLogger.w(TAG, "item.id=${item.id} 已在删除队列中，跳过")
            return
        }

        val currentItems = _uiState.value.mediaItems.toMutableList()
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

    private fun executeQueueForDelete(item: MediaItem, removedIndex: Int, oldIndex: Int) {
        _deleteQueueItems.add(item)
        updateDeleteQueue()

        val currentItems = _uiState.value.mediaItems.toMutableList()
        currentItems.removeAll { it.id == item.id }

        val newIndex = when {
            currentItems.isEmpty() -> 0
            removedIndex < 0 -> oldIndex.coerceAtMost(currentItems.size - 1)
            removedIndex < oldIndex -> (oldIndex - 1).coerceAtLeast(0)
            removedIndex == oldIndex -> oldIndex.coerceAtMost(currentItems.size - 1)
            else -> oldIndex.coerceAtMost(currentItems.size - 1)
        }

        AppLogger.d(TAG, "删除后: oldIndex=$oldIndex, removedIndex=$removedIndex, newIndex=$newIndex, remaining=${currentItems.size}")

        _uiState.update {
            it.copy(
                mediaItems = currentItems,
                currentIndex = newIndex
            )
        }

        if (_deleteQueueItems.size >= DeleteQueue.MAX_QUEUE_SIZE || currentItems.isEmpty()) {
            AppLogger.d(TAG, "触发删除确认对话框")
            _uiState.update { it.copy(showDeleteDialog = true) }
        }
    }

    private fun executeRestoreFromDelete(item: MediaItem, removedIndex: Int, oldIndex: Int) {
        _deleteQueueItems.remove(item)
        updateDeleteQueue()

        val currentItems = _uiState.value.mediaItems.toMutableList()
        // 在原来的位置插入
        val insertIndex = removedIndex.coerceIn(0, currentItems.size)
        currentItems.add(insertIndex, item)

        AppLogger.userAction(TAG, "撤销删除", "item.id=${item.id}, insertIndex=$insertIndex, total=${currentItems.size}")

        _uiState.update {
            it.copy(
                mediaItems = currentItems,
                currentIndex = oldIndex.coerceIn(0, currentItems.size - 1)
            )
        }
    }

    private suspend fun executeKeepCurrent(item: MediaItem, oldIndex: Int) {
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

    fun undo() {
        viewModelScope.launch {
            undoManager.undo()
        }
    }

    fun redo() {
        viewModelScope.launch {
            undoManager.redo()
        }
    }

    fun keepCurrent(silent: Boolean = false) {
        val item = _uiState.value.mediaItems.getOrNull(_uiState.value.currentIndex) ?: return
        if (_uiState.value.showDeleteDialog) return
        val oldIndex = _uiState.value.currentIndex

        viewModelScope.launch {
            try {
                val command = object : UndoCommand {
                    override val description: String = "保留 ${item.name}"

                    override suspend fun execute() {
                        executeKeepCurrent(item, oldIndex)
                    }

                    override suspend fun undo() {
                        restoreKeptItem(item, oldIndex, silent)
                    }
                }
                undoManager.execute(command)
            } catch (e: Exception) {
                AppLogger.exception(TAG, "保留处理失败", e)
                _uiState.update {
                    it.copy(
                        error = e.message ?: "保留处理失败"
                    )
                }
            }
        }
    }

    fun showDeleteConfirmDialog() {
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
                is com.gallery.cleaner.domain.repository.TrashResult.Success -> {
                    AppLogger.i(TAG, "删除成功: ${result.deletedCount} 项")
                    undoManager.clear()
                    _deleteQueueItems.clear()
                    updateDeleteQueue()

                    val isTrashSupported = deleteMediaUseCase.repository.isTrashSupported()
                    if (isTrashSupported && result.deletedCount == 0) {
                        // IntentSender 异步处理，不显示成功提示，等待 onActivityResult 回调
                        AppLogger.d(TAG, "IntentSender 异步处理中，暂不显示成功提示")
                        _uiState.update {
                            it.copy(showDeleteDialog = false)
                        }
                    } else {
                        // 退出时统一持久化保留项
                        finalizeKeptItems()

                        val message = if (isTrashSupported) {
                            "已移入系统回收站 ${result.deletedCount} 项，可在应用回收站或系统相册中查看"
                        } else {
                            "已永久删除 ${result.deletedCount} 项（当前系统不支持回收站）"
                        }

                        _uiState.update {
                            it.copy(
                                showDeleteDialog = false,
                                deleteSuccess = true,
                                deleteMessage = message
                            )
                        }
                    }
                }
                is com.gallery.cleaner.domain.repository.TrashResult.Error -> {
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

    fun onTrashIntentResult(success: Boolean, count: Int) {
        AppLogger.enter(TAG, "onTrashIntentResult", "success" to success, "count" to count)
        if (success) {
            undoManager.clear()
            _deleteQueueItems.clear()
            updateDeleteQueue()

            // 退出时统一持久化保留项
            finalizeKeptItems()

            _uiState.update {
                it.copy(
                    showDeleteDialog = false,
                    deleteSuccess = true,
                    deleteMessage = "已移入系统回收站 $count 项，可在应用回收站或系统相册中查看"
                )
            }
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

    fun clearDeleteQueue() {
        _deleteQueueItems.clear()
        updateDeleteQueue()
    }

    private fun updateDeleteQueue() {
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
