package com.gallery.cleaner.ui.screen.processed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gallery.cleaner.data.repository.ProcessedMediaStore
import com.gallery.cleaner.domain.usecase.GetProcessedMediaGroupsUseCase
import com.gallery.cleaner.ui.screen.gallery.GalleryUiState
import com.gallery.cleaner.util.log.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProcessedGalleryViewModel @Inject constructor(
    private val getProcessedMediaGroupsUseCase: GetProcessedMediaGroupsUseCase,
    private val processedMediaStore: ProcessedMediaStore
) : ViewModel() {

    companion object {
        private const val TAG = "ProcessedGalleryVM"
    }

    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    private var hasLoaded = false

    fun onPermissionGranted() {
        AppLogger.enter(TAG, "onPermissionGranted", "hasLoaded" to hasLoaded, "isLoading" to _uiState.value.isLoading)
        if (hasLoaded && _uiState.value.groups.isNotEmpty()) {
            AppLogger.d(TAG, "已加载过且数据不为空，跳过重复加载")
            return
        }
        if (_uiState.value.isLoading) {
            AppLogger.d(TAG, "正在加载中，跳过")
            return
        }
        hasLoaded = true
        loadProcessedGroups()
    }

    fun loadProcessedGroups() {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            getProcessedMediaGroupsUseCase()
                .onStart {
                    val isFirstBatch = _uiState.value.groups.isEmpty()
                    _uiState.update {
                        it.copy(
                            isLoading = true,
                            isInitialLoading = isFirstBatch,
                            error = null
                        )
                    }
                }
                .catch { error ->
                    AppLogger.exception(TAG, "加载已整理分组异常", error)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isInitialLoading = false,
                            error = error.message ?: "加载失败"
                        )
                    }
                }
                .onCompletion {
                    _uiState.update { it.copy(isLoading = false, isInitialLoading = false) }
                }
                .collect { groups ->
                    AppLogger.dataChange(TAG, "收到已整理分组数据", "count=${groups.size}")
                    _uiState.update {
                        it.copy(
                            groups = groups,
                            isInitialLoading = false,
                            isLoading = false,
                            error = null
                        )
                    }
                    AppLogger.perf(TAG, "加载已整理分组", startTime)
                }
        }
    }

    fun retry() {
        AppLogger.userAction(TAG, "点击重试按钮")
        _uiState.update { it.copy(error = null, isLoading = false, isInitialLoading = false) }
        hasLoaded = false
        loadProcessedGroups()
    }

    fun resetAllProcessed() {
        viewModelScope.launch {
            try {
                processedMediaStore.clearProcessed()
                AppLogger.userAction(TAG, "重置已整理")
            } catch (e: Exception) {
                AppLogger.exception(TAG, "重置已整理失败", e)
                _uiState.update {
                    it.copy(error = e.message ?: "重置已整理失败")
                }
            }
        }
    }
}