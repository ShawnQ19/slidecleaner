package com.gallery.cleaner.ui.screen.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gallery.cleaner.domain.model.MediaItem
import com.gallery.cleaner.domain.repository.MediaRepository
import com.gallery.cleaner.util.log.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrashViewModel @Inject constructor(
    private val repository: MediaRepository
) : ViewModel() {

    companion object {
        private const val TAG = "TrashViewModel"
    }

    private val _uiState = MutableStateFlow(TrashUiState())
    val uiState: StateFlow<TrashUiState> = _uiState.asStateFlow()

    init {
        AppLogger.d(TAG, "TrashViewModel 初始化")
        loadTrashItems()
    }

    fun loadTrashItems() {
        AppLogger.enter(TAG, "loadTrashItems")
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }

                val trashedItems = repository.getTrashedMediaItems()
                AppLogger.i(TAG, "加载到 ${trashedItems.size} 个回收站项目")

                _uiState.update {
                    it.copy(
                        trashItems = trashedItems,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                AppLogger.exception(TAG, "加载回收站失败", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "加载失败"
                    )
                }
            }
            AppLogger.exit(TAG, "loadTrashItems")
        }
    }

    fun enterSelectMode(itemId: Long) {
        _uiState.update {
            it.copy(
                isSelectMode = true,
                selectedItems = setOf(itemId)
            )
        }
    }

    fun exitSelectMode() {
        _uiState.update {
            it.copy(
                isSelectMode = false,
                selectedItems = emptySet()
            )
        }
    }

    fun toggleSelection(itemId: Long) {
        _uiState.update { state ->
            val newSelected = if (itemId in state.selectedItems) {
                state.selectedItems - itemId
            } else {
                state.selectedItems + itemId
            }
            if (newSelected.isEmpty()) {
                state.copy(isSelectMode = false, selectedItems = emptySet())
            } else {
                state.copy(selectedItems = newSelected)
            }
        }
    }

    fun selectAll() {
        _uiState.update { state ->
            state.copy(selectedItems = state.trashItems.map { it.id }.toSet())
        }
    }

    fun selectRange(fromId: Long, toId: Long) {
        _uiState.update { state ->
            val items = state.trashItems
            val fromIndex = items.indexOfFirst { it.id == fromId }
            val toIndex = items.indexOfFirst { it.id == toId }
            if (fromIndex < 0 || toIndex < 0) return@update state

            val start = minOf(fromIndex, toIndex)
            val end = maxOf(fromIndex, toIndex)
            val rangeIds = items.subList(start, end + 1).map { it.id }.toSet()
            state.copy(selectedItems = state.selectedItems + rangeIds)
        }
    }

    fun restoreSelected() {
        viewModelScope.launch {
            try {
                val selectedIds = _uiState.value.selectedItems
                val items = _uiState.value.trashItems.filter { it.id in selectedIds }
                val uris = items.map { it.uri }
                val result = repository.restoreFromTrash(uris)
                when (result) {
                    is com.gallery.cleaner.domain.repository.TrashResult.Success -> {
                        if (result.deletedCount == 0) {
                            AppLogger.i(TAG, "等待系统恢复确认对话框结果")
                        } else {
                            _uiState.update {
                                it.copy(
                                    trashItems = it.trashItems.filter { media -> media.id !in selectedIds },
                                    selectedItems = emptySet(),
                                    isSelectMode = false,
                                    message = "已恢复 ${items.size} 个项目"
                                )
                            }
                        }
                    }
                    is com.gallery.cleaner.domain.repository.TrashResult.Error -> {
                        _uiState.update {
                            it.copy(message = "恢复失败: ${result.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(message = "恢复失败: ${e.message}")
                }
            }
        }
    }

    fun deleteSelected() {
        viewModelScope.launch {
            try {
                val selectedIds = _uiState.value.selectedItems
                val items = _uiState.value.trashItems.filter { it.id in selectedIds }
                val uris = items.map { it.uri }
                val result = repository.deletePermanently(uris)

                when (result) {
                    is com.gallery.cleaner.domain.repository.DeleteResult.Success -> {
                        if (result.deletedCount == 0) {
                            AppLogger.i(TAG, "等待系统永久删除确认对话框结果")
                        } else {
                            _uiState.update {
                                it.copy(
                                    trashItems = it.trashItems.filter { media -> media.id !in selectedIds },
                                    selectedItems = emptySet(),
                                    isSelectMode = false,
                                    message = "已永久删除 ${items.size} 个项目"
                                )
                            }
                        }
                    }
                    is com.gallery.cleaner.domain.repository.DeleteResult.Error -> {
                        _uiState.update {
                            it.copy(message = "删除失败: ${result.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(message = "删除失败: ${e.message}")
                }
            }
        }
    }

    fun restoreFromTrash(item: MediaItem) {
        viewModelScope.launch {
            try {
                val result = repository.restoreFromTrash(listOf(item.uri))
                when (result) {
                    is com.gallery.cleaner.domain.repository.TrashResult.Success -> {
                        if (result.deletedCount == 0) {
                            AppLogger.i(TAG, "等待系统恢复确认对话框结果")
                        } else {
                            _uiState.update {
                                it.copy(
                                    trashItems = it.trashItems.filter { media -> media.id != item.id },
                                    message = "已恢复 1 个项目"
                                )
                            }
                        }
                    }
                    is com.gallery.cleaner.domain.repository.TrashResult.Error -> {
                        _uiState.update {
                            it.copy(message = "恢复失败: ${result.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(message = "恢复失败: ${e.message}")
                }
            }
        }
    }

    fun onRestoreIntentResult(success: Boolean, count: Int) {
        AppLogger.enter(TAG, "onRestoreIntentResult", "success" to success, "count" to count)
        if (success) {
            loadTrashItems()
            _uiState.update {
                it.copy(
                    isSelectMode = false,
                    selectedItems = emptySet(),
                    message = "已恢复 $count 个项目"
                )
            }
        } else {
            _uiState.update {
                it.copy(message = "用户取消了恢复操作")
            }
        }
        AppLogger.exit(TAG, "onRestoreIntentResult")
    }

    fun deleteForever() {
        viewModelScope.launch {
            try {
                val items = _uiState.value.trashItems
                val uris = items.map { it.uri }
                val result = repository.deletePermanently(uris)

                when (result) {
                    is com.gallery.cleaner.domain.repository.DeleteResult.Success -> {
                        if (result.deletedCount == 0) {
                            AppLogger.i(TAG, "等待系统永久删除确认对话框结果")
                        } else {
                            _uiState.update {
                                it.copy(
                                    trashItems = emptyList(),
                                    message = "已永久删除 ${result.deletedCount} 个项目"
                                )
                            }
                        }
                    }
                    is com.gallery.cleaner.domain.repository.DeleteResult.Error -> {
                        _uiState.update {
                            it.copy(message = "删除失败: ${result.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(message = "删除失败: ${e.message}")
                }
            }
        }
    }

    fun onDeleteIntentResult(success: Boolean, count: Int) {
        AppLogger.enter(TAG, "onDeleteIntentResult", "success" to success, "count" to count)
        if (success) {
            _uiState.update {
                it.copy(
                    trashItems = emptyList(),
                    isSelectMode = false,
                    selectedItems = emptySet(),
                    message = "已永久删除 $count 个项目"
                )
            }
        } else {
            _uiState.update {
                it.copy(message = "用户取消了永久删除操作")
            }
        }
        AppLogger.exit(TAG, "onDeleteIntentResult")
    }

    fun dismissMessage() {
        _uiState.update { it.copy(message = "") }
    }
}
