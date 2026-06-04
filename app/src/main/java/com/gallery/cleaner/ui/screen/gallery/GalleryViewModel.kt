package com.gallery.cleaner.ui.screen.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gallery.cleaner.domain.usecase.GetMediaGroupsUseCase
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
class GalleryViewModel @Inject constructor(
    private val getMediaGroupsUseCase: GetMediaGroupsUseCase
) : ViewModel() {

    companion object {
        private const val TAG = "GalleryVM"
    }

    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    private var hasLoaded = false

    /**
     * 权限授予后触发加载。
     * 使用 hasLoaded 标志确保不会重复加载（避免 LaunchedEffect + PermissionHandler 双重触发）。
     */
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
        loadMediaGroups()
    }

    fun loadMediaGroups() {
        AppLogger.enter(TAG, "loadMediaGroups", "isLoading" to _uiState.value.isLoading)
        if (_uiState.value.isLoading) {
            AppLogger.d(TAG, "正在加载中，跳过")
            return
        }

        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            AppLogger.d(TAG, "启动协程收集 Flow，当前线程: ${Thread.currentThread().name}")
            getMediaGroupsUseCase()
                .onStart {
                    AppLogger.d(TAG, "Flow onStart，设置 isLoading=true")
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
                    AppLogger.exception(TAG, "Flow 加载异常", error)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isInitialLoading = false,
                            error = error.message ?: "加载失败"
                        )
                    }
                }
                .onCompletion { cause ->
                    if (cause == null) {
                        AppLogger.d(TAG, "Flow 正常完成，所有批次加载结束")
                    } else {
                        AppLogger.w(TAG, "Flow 异常完成: ${cause.message}", cause)
                    }
                    _uiState.update { it.copy(isLoading = false, isInitialLoading = false) }
                }
                .collect { groups ->
                    AppLogger.dataChange(TAG, "收到媒体分组数据", "count=${groups.size}, thread=${Thread.currentThread().name}")
                    val isFirstBatch = _uiState.value.groups.isEmpty() && groups.isNotEmpty()
                    _uiState.update {
                        it.copy(
                            groups = groups,
                            isInitialLoading = false,
                            error = null
                        )
                    }
                    if (isFirstBatch) {
                        AppLogger.i(TAG, "第一批数据已显示 UI，后续批次将在后台继续加载")
                    }
                    AppLogger.perf(TAG, "加载媒体分组", startTime)
                }
        }
    }

    fun retry() {
        AppLogger.userAction(TAG, "点击重试按钮")
        _uiState.update { it.copy(error = null, isLoading = false, isInitialLoading = false) }
        hasLoaded = false
        loadMediaGroups()
    }
}
