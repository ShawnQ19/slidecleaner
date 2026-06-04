@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.gallery.cleaner.data.repository

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.gallery.cleaner.data.source.MediaStoreDataSource
import com.gallery.cleaner.data.repository.ProcessedMediaStore
import com.gallery.cleaner.domain.model.MediaGroup
import com.gallery.cleaner.domain.model.MediaItem
import com.gallery.cleaner.domain.repository.DeleteResult
import com.gallery.cleaner.domain.repository.MediaRepository
import com.gallery.cleaner.domain.repository.TrashResult
import com.gallery.cleaner.util.ActivityProvider
import com.gallery.cleaner.util.log.AppLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.mapLatest
import javax.inject.Inject
import javax.inject.Singleton
import java.time.YearMonth

@Singleton
class MediaRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataSource: MediaStoreDataSource,
    private val processedMediaStore: ProcessedMediaStore
) : MediaRepository {

    companion object {
        private const val TAG = "MediaRepoImpl"
    }

    private var cachedActiveGroups: List<MediaGroup> = emptyList()
    private var cachedProcessedGroups: List<MediaGroup> = emptyList()

    override fun getMediaGroups(): Flow<List<MediaGroup>> {
        AppLogger.enter(TAG, "getMediaGroups")
        return dataSource.queryMediaBatched()
            .runningFold(emptyList<MediaItem>()) { accumulated, batch ->
                if (accumulated.isEmpty()) {
                    batch
                } else {
                    accumulated + batch
                }
            }
            .drop(1)
            .combine(processedMediaStore.observeProcessedUris()) { allItems, processedUris ->
                val activeItems = filterActiveItems(allItems, processedUris)
                logMediaRange(activeItems)
                val groups = dataSource.groupByMonth(activeItems)
                cachedActiveGroups = groups
                AppLogger.exit(TAG, "getMediaGroups", "groups=${groups.size}")
                groups
            }
            .onEach { groups ->
                cachedActiveGroups = groups
            }
    }

    override fun getProcessedMediaGroups(): Flow<List<MediaGroup>> {
        AppLogger.enter(TAG, "getProcessedMediaGroups")
        return flow {
            // 快速路径：若有缓存则先返回缓存，避免等待全量扫描完成
            if (cachedProcessedGroups.isNotEmpty()) {
                emit(cachedProcessedGroups)
            }

            emitAll(
                processedMediaStore.observeProcessedUris()
                    .mapLatest { processedUris ->
                        val groups = buildProcessedGroups(processedUris)
                        cachedProcessedGroups = groups
                        AppLogger.dataChange(TAG, "已整理分组更新", "groups=${groups.size}")
                        groups
                    }
                    .distinctUntilChanged()
            )
        }
    }

    override suspend fun getMediaItemsByMonth(yearMonth: YearMonth): List<MediaItem> {
        cachedActiveGroups.firstOrNull { it.yearMonth == yearMonth }?.let { cachedGroup ->
            AppLogger.dataChange(TAG, "月份缓存命中", "yearMonth=$yearMonth, items=${cachedGroup.mediaItems.size}")
            return cachedGroup.mediaItems
        }

        AppLogger.w(TAG, "月份缓存未命中，回退到全量查询: $yearMonth")
        val allItems = dataSource.queryMedia().firstOrNull().orEmpty()
        val processedUris = processedMediaStore.getProcessedUris()
        val groups = dataSource.groupByMonth(filterActiveItems(allItems, processedUris))
        cachedActiveGroups = groups
        return groups.firstOrNull { it.yearMonth == yearMonth }?.mediaItems.orEmpty()
    }

    override suspend fun getProcessedMediaItemsByMonth(yearMonth: YearMonth): List<MediaItem> {
        cachedProcessedGroups.firstOrNull { it.yearMonth == yearMonth }?.let { cachedGroup ->
            AppLogger.dataChange(TAG, "已整理缓存命中", "yearMonth=$yearMonth, items=${cachedGroup.mediaItems.size}")
            return cachedGroup.mediaItems
        }

        AppLogger.w(TAG, "已整理缓存未命中，直接按已整理 URI 查询: $yearMonth")
        val processedUris = processedMediaStore.getProcessedUris()
        val groups = buildProcessedGroups(processedUris)
        cachedProcessedGroups = groups
        return groups.firstOrNull { it.yearMonth == yearMonth }?.mediaItems.orEmpty()
    }

    override suspend fun getRandomMediaItems(excludedUris: Set<String>, sampleSize: Int): List<MediaItem> {
        AppLogger.enter(TAG, "getRandomMediaItems", "excluded" to excludedUris.size, "sampleSize" to sampleSize)
        val items = dataSource.getRandomMediaItems(excludedUris, sampleSize)
        AppLogger.exit(TAG, "getRandomMediaItems", "count=${items.size}")
        return items
    }

    private fun filterActiveItems(items: List<MediaItem>, processedUris: Set<String>): List<MediaItem> {
        if (processedUris.isEmpty()) return items
        return items.filterNot { it.uri.toString() in processedUris }
    }

    private fun filterProcessedItems(items: List<MediaItem>, processedUris: Set<String>): List<MediaItem> {
        if (processedUris.isEmpty()) return emptyList()
        return items.filter { it.uri.toString() in processedUris }
    }

    private suspend fun buildProcessedGroups(processedUris: Set<String>): List<MediaGroup> {
        if (processedUris.isEmpty()) return emptyList()

        // 只查询仍存在于 MediaStore 中的已整理项（不含回收站）
        // 已删除的照片不应出现在已整理相册中
        val processedItems = dataSource.getMediaItemsByUris(processedUris.map(Uri::parse))
        if (processedItems.isNotEmpty()) {
            logMediaRange(processedItems)
        }

        AppLogger.i(TAG, "buildProcessedGroups: processedUris=${processedUris.size}, found=${processedItems.size}")
        return dataSource.groupByMonth(processedItems)
    }

    private fun logMediaRange(items: List<MediaItem>) {
        if (items.isEmpty()) return
        val earliest = items.minOf { it.dateAdded }
        val latest = items.maxOf { it.dateAdded }
        val earliestDate = java.time.LocalDateTime.ofInstant(
            java.time.Instant.ofEpochSecond(earliest),
            java.time.ZoneId.systemDefault()
        )
        val latestDate = java.time.LocalDateTime.ofInstant(
            java.time.Instant.ofEpochSecond(latest),
            java.time.ZoneId.systemDefault()
        )
        AppLogger.i(TAG, "照片时间范围: $earliestDate ~ $latestDate")
    }

    override suspend fun moveToTrash(uris: List<Uri>): TrashResult {
        AppLogger.enter(TAG, "moveToTrash", "count" to uris.size)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                // Android R+ 使用 createTrashRequest 将文件移入系统回收站
                // 这个 API 会触发系统权限对话框，让用户确认移入回收站
                val pendingIntent = MediaStore.createTrashRequest(
                    context.contentResolver,
                    uris,
                    true  // true = 移入回收站
                )

                // 直接启动系统回收站确认对话框
                // createTrashRequest 的 IntentSender 会由系统处理，用户确认后系统会自动设置 IS_TRASHED=1
                AppLogger.i(TAG, "启动系统回收站确认对话框，共 ${uris.size} 项")
                try {
                    val activity = ActivityProvider.currentActivity
                    if (activity != null) {
                        // 保存待处理的 URI 列表到 MainActivity，以便在 onActivityResult 中使用
                        com.gallery.cleaner.MainActivity.pendingTrashUris = uris.toList()
                        val intentSender = pendingIntent.intentSender
                        activity.startIntentSenderForResult(
                            intentSender,
                            com.gallery.cleaner.MainActivity.REQUEST_CODE_TRASH,
                            null, 0, 0, 0
                        )
                        // 注意：IntentSender 是异步的，结果通过 onActivityResult 返回
                        // 系统会在用户确认后自动处理 IS_TRASHED 标记
                        AppLogger.i(TAG, "已启动系统回收站确认对话框")
                        return TrashResult.Success(0) // 返回0表示异步处理，实际结果通过onActivityResult返回
                    } else {
                        AppLogger.e(TAG, "当前没有可用的 Activity，无法启动 IntentSender")
                        TrashResult.Error("无法启动删除对话框")
                    }
                } catch (e: Exception) {
                    AppLogger.exception(TAG, "启动回收站对话框失败", e)
                    TrashResult.Error("启动删除对话框失败: ${e.message}")
                }
            } catch (e: Exception) {
                AppLogger.exception(TAG, "创建回收站请求失败", e)
                TrashResult.Error(e.message ?: "移入回收站失败")
            }
        } else {
            // Android 10 及以下不支持回收站，直接永久删除
            deletePermanently(uris).let { result ->
                when (result) {
                    is DeleteResult.Success -> TrashResult.Success(result.deletedCount)
                    is DeleteResult.Error -> TrashResult.Error(result.message)
                }
            }
        }
    }

    override suspend fun restoreFromTrash(uris: List<Uri>): TrashResult {
        AppLogger.enter(TAG, "restoreFromTrash", "count" to uris.size)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val pendingIntent = MediaStore.createTrashRequest(
                    context.contentResolver,
                    uris,
                    false
                )
                AppLogger.i(TAG, "启动系统恢复确认对话框，共 ${uris.size} 项")
                try {
                    val activity = ActivityProvider.currentActivity
                    if (activity != null) {
                        val intentSender = pendingIntent.intentSender
                        activity.startIntentSenderForResult(
                            intentSender,
                            com.gallery.cleaner.MainActivity.REQUEST_CODE_RESTORE,
                            null, 0, 0, 0
                        )
                        AppLogger.i(TAG, "已启动系统恢复确认对话框")
                        TrashResult.Success(0)
                    } else {
                        AppLogger.e(TAG, "当前没有可用的 Activity，无法启动恢复对话框")
                        TrashResult.Error("无法启动恢复对话框")
                    }
                } catch (e: Exception) {
                    AppLogger.exception(TAG, "启动恢复对话框失败", e)
                    TrashResult.Error("启动恢复对话框失败: ${e.message}")
                }
            } catch (e: Exception) {
                AppLogger.exception(TAG, "创建恢复请求失败", e)
                TrashResult.Error(e.message ?: "恢复失败")
            }
        } else {
            AppLogger.w(TAG, "当前系统版本不支持回收站功能")
            TrashResult.Error("当前系统版本不支持回收站功能")
        }
    }

    override suspend fun deletePermanently(uris: List<Uri>): DeleteResult {
        AppLogger.enter(TAG, "deletePermanently", "count" to uris.size)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return try {
                val pendingIntent = MediaStore.createDeleteRequest(
                    context.contentResolver,
                    uris
                )
                AppLogger.i(TAG, "启动系统永久删除确认对话框，共 ${uris.size} 项")
                try {
                    val activity = ActivityProvider.currentActivity
                    if (activity != null) {
                        com.gallery.cleaner.MainActivity.pendingDeleteUris = uris.toList()
                        val intentSender = pendingIntent.intentSender
                        activity.startIntentSenderForResult(
                            intentSender,
                            com.gallery.cleaner.MainActivity.REQUEST_CODE_DELETE,
                            null, 0, 0, 0
                        )
                        AppLogger.i(TAG, "已启动系统永久删除确认对话框")
                        DeleteResult.Success(0)
                    } else {
                        AppLogger.e(TAG, "当前没有可用的 Activity，无法启动删除对话框")
                        DeleteResult.Error("无法启动删除对话框")
                    }
                } catch (e: Exception) {
                    AppLogger.exception(TAG, "启动永久删除对话框失败", e)
                    DeleteResult.Error("启动删除对话框失败: ${e.message}")
                }
            } catch (e: Exception) {
                AppLogger.exception(TAG, "创建永久删除请求失败", e)
                DeleteResult.Error(e.message ?: "永久删除失败")
            }
        } else {
            var deletedCount = 0
            var errorMessage = ""

            for (uri in uris) {
                try {
                    val rows = context.contentResolver.delete(uri, null, null)
                    if (rows > 0) deletedCount++
                } catch (e: Exception) {
                    errorMessage = e.message ?: "删除失败"
                    AppLogger.e(TAG, "永久删除失败: $uri", e)
                }
            }

            return if (deletedCount > 0) {
                AppLogger.exit(TAG, "deletePermanently", "deleted=$deletedCount")
                DeleteResult.Success(deletedCount)
            } else {
                AppLogger.e(TAG, "deletePermanently 全部失败: $errorMessage")
                DeleteResult.Error(errorMessage)
            }
        }
    }

    override fun isTrashSupported(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    }

    override suspend fun getMediaItemById(id: Long): MediaItem? {
        return dataSource.getMediaItemById(id)
    }

    override suspend fun getTrashedMediaItems(): List<MediaItem> {
        return dataSource.queryTrashedMedia()
    }
}
