package com.gallery.cleaner.data.source

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import com.gallery.cleaner.domain.model.LivePhotoInfo
import com.gallery.cleaner.domain.model.MediaItem
import com.gallery.cleaner.util.LivePhotoDetector
import com.gallery.cleaner.util.log.AppLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.random.Random
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaStoreDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val BATCH_SIZE = 600
    }

    fun queryMediaBatched(): Flow<List<MediaItem>> = flow {
        AppLogger.enter("MediaStoreDS", "queryMediaBatched")
        val startTime = System.currentTimeMillis()

        val allItems = mutableListOf<MediaItem>()
        var imageOffset = 0
        var videoOffset = 0
        var hasMoreImages = true
        var hasMoreVideos = true
        var totalLoaded = 0
        var batchIndex = 0

        while (hasMoreImages || hasMoreVideos) {
            val batch = mutableListOf<MediaItem>()

            if (hasMoreImages) {
                try {
                    val beforeSize = batch.size
                    queryImagesBatch(batch, imageOffset, BATCH_SIZE)
                    val imageCount = batch.size - beforeSize
                    if (imageCount < BATCH_SIZE) {
                        hasMoreImages = false
                    }
                    imageOffset += BATCH_SIZE
                } catch (e: SecurityException) {
                    AppLogger.exception("MediaStoreDS", "查询图片权限被拒绝", e)
                    hasMoreImages = false
                } catch (e: Exception) {
                    AppLogger.exception("MediaStoreDS", "查询图片失败", e)
                    hasMoreImages = false
                }
            }

            if (hasMoreVideos) {
                try {
                    val beforeSize = batch.size
                    queryVideosBatch(batch, videoOffset, BATCH_SIZE)
                    val videoCount = batch.size - beforeSize
                    if (videoCount < BATCH_SIZE) {
                        hasMoreVideos = false
                    }
                    videoOffset += BATCH_SIZE
                } catch (e: SecurityException) {
                    AppLogger.exception("MediaStoreDS", "查询视频权限被拒绝", e)
                    hasMoreVideos = false
                } catch (e: Exception) {
                    AppLogger.exception("MediaStoreDS", "查询视频失败", e)
                    hasMoreVideos = false
                }
            }

            if (batch.isEmpty()) break

            allItems.addAll(batch)
            totalLoaded += batch.size
            batchIndex++
            val sortedBatch = batch.sortedByDescending { it.dateAdded }
            AppLogger.dataChange("MediaStoreDS", "第 $batchIndex 批加载完成", "batch=${sortedBatch.size}, total=$totalLoaded, imgOffset=$imageOffset, vidOffset=$videoOffset")
            emit(sortedBatch)
        }

        AppLogger.perf("MediaStoreDS", "分批加载媒体完成", startTime)
        AppLogger.exit("MediaStoreDS", "queryMediaBatched", "total=$totalLoaded")
    }.flowOn(Dispatchers.IO)

    fun queryMedia(): Flow<List<MediaItem>> = flow {
        AppLogger.enter("MediaStoreDS", "queryMedia")
        val startTime = System.currentTimeMillis()

        val items = mutableListOf<MediaItem>()

        try {
            AppLogger.i("MediaStoreDS", "开始查询图片...")
            queryImages(items)
            AppLogger.i("MediaStoreDS", "图片查询完成，当前共 ${items.size} 张")
        } catch (e: SecurityException) {
            AppLogger.exception("MediaStoreDS", "查询图片权限被拒绝", e)
        } catch (e: Exception) {
            AppLogger.exception("MediaStoreDS", "查询图片失败", e)
        }

        try {
            AppLogger.i("MediaStoreDS", "开始查询视频...")
            queryVideos(items)
            AppLogger.i("MediaStoreDS", "视频查询完成，当前共 ${items.size} 个媒体项")
        } catch (e: SecurityException) {
            AppLogger.exception("MediaStoreDS", "查询视频权限被拒绝", e)
        } catch (e: Exception) {
            AppLogger.exception("MediaStoreDS", "查询视频失败", e)
        }

        val filesCount = queryFilesCount()
        AppLogger.i("MediaStoreDS", "诊断: Images+Videos=${items.size}, Files总计数=$filesCount")

        if (items.size < filesCount && filesCount > 0) {
            AppLogger.w("MediaStoreDS", "检测到 Images+Videos 数量(${items.size})少于 Files 计数($filesCount)，使用 Files 查询补充")
            queryAllMediaViaFiles(items)
        }

        val sortedItems = items.distinctBy { it.id }.sortedByDescending { it.dateAdded }
        AppLogger.dataChange("MediaStoreDS", "查询全部完成", "count=${sortedItems.size}")
        emit(sortedItems)
        AppLogger.perf("MediaStoreDS", "queryMedia", startTime)
        AppLogger.exit("MediaStoreDS", "queryMedia")
    }.flowOn(Dispatchers.IO)

    suspend fun getRandomMediaItems(excludedUris: Set<String>, sampleSize: Int): List<MediaItem> = withContext(Dispatchers.IO) {
        AppLogger.enter("MediaStoreDS", "getRandomMediaItems", "excluded" to excludedUris.size, "sampleSize" to sampleSize)

        if (sampleSize <= 0) return@withContext emptyList()

        val reservoir = mutableListOf<Uri>()
        var seenCount = 0

        seenCount = sampleRandomImageUris(excludedUris, reservoir, seenCount, sampleSize)
        seenCount = sampleRandomVideoUris(excludedUris, reservoir, seenCount, sampleSize)

        val sampledItems = reservoir.mapNotNull { uri -> getMediaItemByUri(uri) }
        AppLogger.exit("MediaStoreDS", "getRandomMediaItems", "seen=$seenCount, sampled=${sampledItems.size}")
        sampledItems
    }

    suspend fun getMediaItemsByUris(uris: Collection<Uri>): List<MediaItem> = withContext(Dispatchers.IO) {
        AppLogger.enter("MediaStoreDS", "getMediaItemsByUris", "count" to uris.size)

        if (uris.isEmpty()) return@withContext emptyList()

        val imageIds = mutableListOf<Long>()
        val videoIds = mutableListOf<Long>()
        val fallbackUris = mutableListOf<Uri>()

        uris.forEach { uri ->
            val id = uri.lastPathSegment?.toLongOrNull()
            if (id == null) {
                fallbackUris.add(uri)
                return@forEach
            }

            when (uri.pathSegments.getOrNull(1)) {
                "images" -> imageIds.add(id)
                "video" -> videoIds.add(id)
                else -> fallbackUris.add(uri)
            }
        }

        val items = mutableListOf<MediaItem>()
        items.addAll(queryMediaItemsByIds(imageIds, isVideo = false))
        items.addAll(queryMediaItemsByIds(videoIds, isVideo = true))

        fallbackUris.forEach { uri ->
            try {
                getMediaItemByUri(uri)?.let(items::add)
            } catch (e: Exception) {
                AppLogger.w("MediaStoreDS", "Fallback query failed: $uri -> ${e.message}")
            }
        }

        val distinctItems = items.distinctBy { it.uri.toString() }.sortedByDescending { it.dateAdded }
        AppLogger.exit("MediaStoreDS", "getMediaItemsByUris", "count=${distinctItems.size}")
        distinctItems
    }

    private fun queryMediaItemsByIds(ids: List<Long>, isVideo: Boolean): List<MediaItem> = buildList {
        if (ids.isEmpty()) return@buildList

        val collection = if (isVideo) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
        }

        val projection = if (isVideo) {
            arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.MIME_TYPE,
                MediaStore.MediaColumns.DATE_TAKEN,
                MediaStore.MediaColumns.DATE_ADDED,
                MediaStore.MediaColumns.SIZE,
                MediaStore.MediaColumns.WIDTH,
                MediaStore.MediaColumns.HEIGHT,
                MediaStore.Video.Media.DURATION
            )
        } else {
            arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.MIME_TYPE,
                MediaStore.MediaColumns.DATE_TAKEN,
                MediaStore.MediaColumns.DATE_ADDED,
                MediaStore.MediaColumns.SIZE,
                MediaStore.MediaColumns.WIDTH,
                MediaStore.MediaColumns.HEIGHT
            )
        }

        ids.chunked(200).forEach { batchIds ->
            try {
                val placeholders = batchIds.joinToString(",") { "?" }
                val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    "${MediaStore.MediaColumns._ID} IN ($placeholders) AND ${MediaStore.MediaColumns.IS_TRASHED} = 0"
                } else {
                    "${MediaStore.MediaColumns._ID} IN ($placeholders)"
                }
                val selectionArgs = batchIds.map { it.toString() }.toTypedArray()

                context.contentResolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
                    val idIdx = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                    val nameIdx = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                    val mimeIdx = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
                    val dateTakenIdx = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_TAKEN)
                    val dateIdx = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
                    val sizeIdx = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                    val widthIdx = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.WIDTH)
                    val heightIdx = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.HEIGHT)
                    val durationIdx = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)

                    while (cursor.moveToNext()) {
                        try {
                            val id = cursor.getLong(idIdx)
                            val uri = ContentUris.withAppendedId(collection, id)
                            val mimeType = cursor.getString(mimeIdx) ?: if (isVideo) "video/*" else "image/*"
                            val dateAdded = resolveDisplayTimestampSeconds(
                                uri = uri,
                                dateTakenMillis = if (dateTakenIdx >= 0 && !cursor.isNull(dateTakenIdx)) cursor.getLong(dateTakenIdx) else null,
                                dateAddedSeconds = cursor.getLong(dateIdx),
                                isVideo = isVideo
                            )

                            add(
                                MediaItem(
                                    id = id,
                                    uri = uri,
                                    name = cursor.getString(nameIdx) ?: "",
                                    mimeType = mimeType,
                                    dateAdded = dateAdded,
                                    size = cursor.getLong(sizeIdx),
                                    width = cursor.getInt(widthIdx),
                                    height = cursor.getInt(heightIdx),
                                    duration = if (isVideo && durationIdx >= 0 && !cursor.isNull(durationIdx)) {
                                        cursor.getLong(durationIdx)
                                    } else null,
                                    livePhotoInfo = if (isVideo) LivePhotoInfo() else LivePhotoDetector.detect(context, uri)
                                )
                            )
                        } catch (e: Exception) {
                            AppLogger.w("MediaStoreDS", "Batch row parse failed: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                AppLogger.w("MediaStoreDS", "Batch URI query failed: ${e.message}")
            }
        }
    }

    private fun queryFilesCount(): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return 0
        val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val selection = "${MediaStore.Files.FileColumns.IS_TRASHED} = ? AND (${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?)"
        val selectionArgs = arrayOf("0",
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
        )
        return try {
            context.contentResolver.query(collection, arrayOf("count(*)"), selection, selectionArgs, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getInt(0) else 0
            } ?: 0
        } catch (e: Exception) {
            AppLogger.w("MediaStoreDS", "Files count query failed: ${e.message}")
            0
        }
    }

    private fun sampleRandomImageUris(
        excludedUris: Set<String>,
        reservoir: MutableList<Uri>,
        seenCount: Int,
        sampleSize: Int
    ): Int {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            "${MediaStore.Images.Media.IS_TRASHED} = 0"
        } else null
        val cursor = context.contentResolver.query(collection, projection, selection, null, null)

        var seen = seenCount
        cursor?.use {
            val idIdx = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while (it.moveToNext()) {
                val uri = ContentUris.withAppendedId(collection, it.getLong(idIdx))
                if (uri.toString() in excludedUris) continue
                seen = reservoirSample(reservoir, uri, seen, sampleSize)
            }
        }

        return seen
    }

    private fun sampleRandomVideoUris(
        excludedUris: Set<String>,
        reservoir: MutableList<Uri>,
        seenCount: Int,
        sampleSize: Int
    ): Int {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
        val projection = arrayOf(MediaStore.Video.Media._ID)
        val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            "${MediaStore.Video.Media.IS_TRASHED} = 0"
        } else null
        val cursor = context.contentResolver.query(collection, projection, selection, null, null)

        var seen = seenCount
        cursor?.use {
            val idIdx = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            while (it.moveToNext()) {
                val uri = ContentUris.withAppendedId(collection, it.getLong(idIdx))
                if (uri.toString() in excludedUris) continue
                seen = reservoirSample(reservoir, uri, seen, sampleSize)
            }
        }

        return seen
    }

    private fun reservoirSample(
        reservoir: MutableList<Uri>,
        candidate: Uri,
        seenCount: Int,
        sampleSize: Int
    ): Int {
        val nextSeen = seenCount + 1
        if (reservoir.size < sampleSize) {
            reservoir.add(candidate)
        } else {
            val replaceIndex = Random.nextInt(nextSeen)
            if (replaceIndex < sampleSize) {
                reservoir[replaceIndex] = candidate
            }
        }
        return nextSeen
    }

    private fun queryAllMediaViaFiles(existingItems: MutableList<MediaItem>) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.MediaColumns.DATE_TAKEN,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.WIDTH,
            MediaStore.Files.FileColumns.HEIGHT,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.Files.FileColumns.DURATION
        )
        val selection = "${MediaStore.Files.FileColumns.IS_TRASHED} = ? AND (${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?)"
        val selectionArgs = arrayOf("0",
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
        )
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"

        try {
            context.contentResolver.query(collection, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
                AppLogger.i("MediaStoreDS", "Files 查询 cursor 行数: ${cursor.count}")
                val existingIds = existingItems.map { it.id }.toSet()
                var addedCount = 0
                val idIdx = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameIdx = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val mimeIdx = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
                val dateTakenIdx = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_TAKEN)
                val dateIdx = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
                val sizeIdx = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val widthIdx = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.WIDTH)
                val heightIdx = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.HEIGHT)
                val mediaTypeIdx = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
                val durationIdx = cursor.getColumnIndex(MediaStore.Files.FileColumns.DURATION)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIdx)
                    if (id in existingIds) continue
                    val mediaType = cursor.getInt(mediaTypeIdx)
                    val isVideo = mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                    val uri = if (isVideo) {
                        ContentUris.withAppendedId(MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL), id)
                    } else {
                        ContentUris.withAppendedId(MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL), id)
                    }
                    val dateAdded = resolveDisplayTimestampSeconds(
                        uri = uri,
                        dateTakenMillis = if (dateTakenIdx >= 0 && !cursor.isNull(dateTakenIdx)) cursor.getLong(dateTakenIdx) else null,
                        dateAddedSeconds = cursor.getLong(dateIdx),
                        isVideo = isVideo
                    )
                    existingItems.add(
                        MediaItem(
                            id = id,
                            uri = uri,
                            name = cursor.getString(nameIdx) ?: "",
                            mimeType = cursor.getString(mimeIdx) ?: if (isVideo) "video/*" else "image/*",
                            dateAdded = dateAdded,
                            size = cursor.getLong(sizeIdx),
                            width = cursor.getInt(widthIdx),
                            height = cursor.getInt(heightIdx),
                            duration = if (durationIdx >= 0 && !cursor.isNull(durationIdx)) cursor.getLong(durationIdx) else null,
                            livePhotoInfo = LivePhotoInfo()
                        )
                    )
                    addedCount++
                }
                AppLogger.i("MediaStoreDS", "Files 查询补充了 $addedCount 个缺失项")
            }
        } catch (e: Exception) {
            AppLogger.exception("MediaStoreDS", "Files 查询失败", e)
        }
    }

    private fun queryImagesBatch(items: MutableList<MediaItem>, offset: Int, limit: Int) {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.MediaColumns.DATE_TAKEN,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT
        )

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val cursor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val bundle = Bundle().apply {
                    putString(
                        ContentResolver.QUERY_ARG_SQL_SELECTION,
                        "${MediaStore.Images.Media.IS_TRASHED} = 0"
                    )
                    putStringArray(
                        ContentResolver.QUERY_ARG_SORT_COLUMNS,
                        arrayOf(MediaStore.Images.Media.DATE_ADDED)
                    )
                    putInt(
                        ContentResolver.QUERY_ARG_SORT_DIRECTION,
                        ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
                    )
                    putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
                    putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
                }
                context.contentResolver.query(collection, projection, bundle, null)
            } catch (e: Exception) {
                AppLogger.w("MediaStoreDS", "Bundle query failed, falling back: ${e.message}")
                context.contentResolver.query(
                    collection, projection, null, null,
                    "${MediaStore.Images.Media.DATE_ADDED} DESC LIMIT $limit OFFSET $offset"
                )
            }
        } else {
            context.contentResolver.query(
                collection, projection, null, null,
                "${MediaStore.Images.Media.DATE_ADDED} DESC LIMIT $limit OFFSET $offset"
            )
        }

        cursor?.use {
            val idIdx = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameIdx = it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val mimeIdx = it.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
            val dateTakenIdx = it.getColumnIndex(MediaStore.MediaColumns.DATE_TAKEN)
            val dateIdx = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val sizeIdx = it.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val widthIdx = it.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightIdx = it.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)

            while (it.moveToNext()) {
                try {
                    val id = it.getLong(idIdx)
                    val uri = ContentUris.withAppendedId(collection, id)
                    val dateAdded = resolveDisplayTimestampSeconds(
                        uri = uri,
                        dateTakenMillis = if (dateTakenIdx >= 0 && !it.isNull(dateTakenIdx)) it.getLong(dateTakenIdx) else null,
                        dateAddedSeconds = it.getLong(dateIdx),
                        isVideo = false
                    )
                    items.add(
                        MediaItem(
                            id = id,
                            uri = uri,
                            name = it.getString(nameIdx) ?: "",
                            mimeType = it.getString(mimeIdx) ?: "image/*",
                            dateAdded = dateAdded,
                            size = it.getLong(sizeIdx),
                            width = it.getInt(widthIdx),
                            height = it.getInt(heightIdx),
                            duration = null,
                            livePhotoInfo = LivePhotoInfo()
                        )
                    )
                } catch (e: Exception) {
                    AppLogger.w("MediaStoreDS", "Error reading image item: ${e.message}")
                }
            }
        } ?: AppLogger.w("MediaStoreDS", "Image batch query returned null cursor")
    }

    private fun queryVideosBatch(items: MutableList<MediaItem>, offset: Int, limit: Int) {
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.MediaColumns.DATE_TAKEN,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.DURATION
        )

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val cursor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val bundle = Bundle().apply {
                    putString(
                        ContentResolver.QUERY_ARG_SQL_SELECTION,
                        "${MediaStore.Video.Media.IS_TRASHED} = 0"
                    )
                    putStringArray(
                        ContentResolver.QUERY_ARG_SORT_COLUMNS,
                        arrayOf(MediaStore.Video.Media.DATE_ADDED)
                    )
                    putInt(
                        ContentResolver.QUERY_ARG_SORT_DIRECTION,
                        ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
                    )
                    putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
                    putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
                }
                context.contentResolver.query(collection, projection, bundle, null)
            } catch (e: Exception) {
                AppLogger.w("MediaStoreDS", "Bundle query failed, falling back: ${e.message}")
                context.contentResolver.query(
                    collection, projection, null, null,
                    "${MediaStore.Video.Media.DATE_ADDED} DESC LIMIT $limit OFFSET $offset"
                )
            }
        } else {
            context.contentResolver.query(
                collection, projection, null, null,
                "${MediaStore.Video.Media.DATE_ADDED} DESC LIMIT $limit OFFSET $offset"
            )
        }

        cursor?.use {
            val idIdx = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameIdx = it.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val mimeIdx = it.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
            val dateTakenIdx = it.getColumnIndex(MediaStore.MediaColumns.DATE_TAKEN)
            val dateIdx = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val sizeIdx = it.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val widthIdx = it.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
            val heightIdx = it.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
            val durationIdx = it.getColumnIndex(MediaStore.Video.Media.DURATION)

            while (it.moveToNext()) {
                try {
                    val id = it.getLong(idIdx)
                    val uri = ContentUris.withAppendedId(collection, id)
                    val dateAdded = resolveDisplayTimestampSeconds(
                        uri = uri,
                        dateTakenMillis = if (dateTakenIdx >= 0 && !it.isNull(dateTakenIdx)) it.getLong(dateTakenIdx) else null,
                        dateAddedSeconds = it.getLong(dateIdx),
                        isVideo = true
                    )
                    items.add(
                        MediaItem(
                            id = id,
                            uri = uri,
                            name = it.getString(nameIdx) ?: "",
                            mimeType = it.getString(mimeIdx) ?: "video/*",
                            dateAdded = dateAdded,
                            size = it.getLong(sizeIdx),
                            width = it.getInt(widthIdx),
                            height = it.getInt(heightIdx),
                            duration = if (durationIdx >= 0 && !it.isNull(durationIdx)) {
                                it.getLong(durationIdx)
                            } else null,
                            livePhotoInfo = LivePhotoInfo()
                        )
                    )
                } catch (e: Exception) {
                    AppLogger.w("MediaStoreDS", "Error reading video item: ${e.message}")
                }
            }
        } ?: AppLogger.w("MediaStoreDS", "Video batch query returned null cursor")
    }

    private fun queryImages(items: MutableList<MediaItem>) {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.MediaColumns.DATE_TAKEN,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT
        )

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        AppLogger.i("MediaStoreDS", "查询图片, URI=$collection, SDK=${Build.VERSION.SDK_INT}, 设备=${Build.MANUFACTURER} ${Build.MODEL}")

        val selection = "${MediaStore.Images.Media.IS_TRASHED} = ?"
        val selectionArgs = arrayOf("0")
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        val cursor = context.contentResolver.query(
            collection, projection, selection, selectionArgs, sortOrder
        )

        cursor?.use {
            AppLogger.i("MediaStoreDS", "图片 cursor 行数: ${it.count}")
            var minDate = Long.MAX_VALUE
            var maxDate = Long.MIN_VALUE
            val idIdx = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameIdx = it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val mimeIdx = it.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
            val dateTakenIdx = it.getColumnIndex(MediaStore.MediaColumns.DATE_TAKEN)
            val dateIdx = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val sizeIdx = it.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val widthIdx = it.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightIdx = it.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)

            while (it.moveToNext()) {
                try {
                    val id = it.getLong(idIdx)
                    val uri = ContentUris.withAppendedId(collection, id)
                    val dateAdded = resolveDisplayTimestampSeconds(
                        uri = uri,
                        dateTakenMillis = if (dateTakenIdx >= 0 && !it.isNull(dateTakenIdx)) it.getLong(dateTakenIdx) else null,
                        dateAddedSeconds = it.getLong(dateIdx),
                        isVideo = false
                    )
                    if (dateAdded < minDate) minDate = dateAdded
                    if (dateAdded > maxDate) maxDate = dateAdded
                    items.add(
                        MediaItem(
                            id = id,
                            uri = uri,
                            name = it.getString(nameIdx) ?: "",
                            mimeType = it.getString(mimeIdx) ?: "image/*",
                            dateAdded = dateAdded,
                            size = it.getLong(sizeIdx),
                            width = it.getInt(widthIdx),
                            height = it.getInt(heightIdx),
                            duration = null,
                            livePhotoInfo = LivePhotoInfo()
                        )
                    )
                } catch (e: Exception) {
                    AppLogger.w("MediaStoreDS", "Error reading image item: ${e.message}")
                }
            }
            if (minDate != Long.MAX_VALUE) {
                val earliest = LocalDateTime.ofInstant(Instant.ofEpochSecond(minDate), ZoneId.systemDefault())
                val latest = LocalDateTime.ofInstant(Instant.ofEpochSecond(maxDate), ZoneId.systemDefault())
                AppLogger.i("MediaStoreDS", "图片时间范围: $earliest ~ $latest, 共 ${items.size} 张")
            }
        } ?: AppLogger.w("MediaStoreDS", "Image query returned null cursor")
    }

    suspend fun getMediaItemByUri(uri: Uri): MediaItem? = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.DATE_TAKEN,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT,
            MediaStore.Video.Media.DURATION
        )

        val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "${MediaStore.MediaColumns.IS_TRASHED} = 0"
        } else null
        context.contentResolver.query(uri, projection, selection, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null

            val mimeTypeIdx = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
            val mimeType = if (mimeTypeIdx >= 0) cursor.getString(mimeTypeIdx) else null
            val isVideo = mimeType?.startsWith("video/") == true
            val durationIdx = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
            val dateTakenIdx = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_TAKEN)
            val dateAddedIdx = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED)

            val resolvedMimeType = mimeType ?: if (isVideo) "video/*" else "image/*"
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
            MediaItem(
                id = id,
                uri = uri,
                name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)) ?: "",
                mimeType = resolvedMimeType,
                dateAdded = resolveDisplayTimestampSeconds(
                    uri = uri,
                    dateTakenMillis = if (dateTakenIdx >= 0 && !cursor.isNull(dateTakenIdx)) cursor.getLong(dateTakenIdx) else null,
                    dateAddedSeconds = if (dateAddedIdx >= 0) cursor.getLong(dateAddedIdx) else 0L,
                    isVideo = isVideo
                ),
                size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)),
                width = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.WIDTH)),
                height = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.HEIGHT)),
                duration = if (isVideo && durationIdx >= 0 && !cursor.isNull(durationIdx)) {
                    cursor.getLong(durationIdx)
                } else null,
                livePhotoInfo = if (isVideo) LivePhotoInfo() else LivePhotoDetector.detect(context, uri)
            )
        }
    }

    private fun queryVideos(items: MutableList<MediaItem>) {
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.MediaColumns.DATE_TAKEN,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.DURATION
        )

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
        AppLogger.i("MediaStoreDS", "查询视频, URI=$collection")

        val selection = "${MediaStore.Video.Media.IS_TRASHED} = ?"
        val selectionArgs = arrayOf("0")
        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        val cursor = context.contentResolver.query(
            collection, projection, selection, selectionArgs, sortOrder
        )

        cursor?.use {
            AppLogger.i("MediaStoreDS", "视频 cursor 行数: ${it.count}")
            var minDate = Long.MAX_VALUE
            var maxDate = Long.MIN_VALUE
            val idIdx = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameIdx = it.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val mimeIdx = it.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
            val dateTakenIdx = it.getColumnIndex(MediaStore.MediaColumns.DATE_TAKEN)
            val dateIdx = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val sizeIdx = it.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val widthIdx = it.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
            val heightIdx = it.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
            val durationIdx = it.getColumnIndex(MediaStore.Video.Media.DURATION)

            while (it.moveToNext()) {
                try {
                    val id = it.getLong(idIdx)
                    val uri = ContentUris.withAppendedId(collection, id)
                    val dateAdded = resolveDisplayTimestampSeconds(
                        uri = uri,
                        dateTakenMillis = if (dateTakenIdx >= 0 && !it.isNull(dateTakenIdx)) it.getLong(dateTakenIdx) else null,
                        dateAddedSeconds = it.getLong(dateIdx),
                        isVideo = true
                    )
                    if (dateAdded < minDate) minDate = dateAdded
                    if (dateAdded > maxDate) maxDate = dateAdded
                    items.add(
                        MediaItem(
                            id = id,
                            uri = uri,
                            name = it.getString(nameIdx) ?: "",
                            mimeType = it.getString(mimeIdx) ?: "video/*",
                            dateAdded = dateAdded,
                            size = it.getLong(sizeIdx),
                            width = it.getInt(widthIdx),
                            height = it.getInt(heightIdx),
                            duration = if (durationIdx >= 0 && !it.isNull(durationIdx)) {
                                it.getLong(durationIdx)
                            } else null,
                            livePhotoInfo = LivePhotoInfo()
                        )
                    )
                } catch (e: Exception) {
                    AppLogger.w("MediaStoreDS", "Error reading video item: ${e.message}")
                }
            }
            if (minDate != Long.MAX_VALUE) {
                val earliest = LocalDateTime.ofInstant(Instant.ofEpochSecond(minDate), ZoneId.systemDefault())
                val latest = LocalDateTime.ofInstant(Instant.ofEpochSecond(maxDate), ZoneId.systemDefault())
                AppLogger.i("MediaStoreDS", "视频时间范围: $earliest ~ $latest")
            }
        } ?: AppLogger.w("MediaStoreDS", "Video query returned null cursor")
    }

    fun groupByMonth(items: List<MediaItem>): List<com.gallery.cleaner.domain.model.MediaGroup> {
        AppLogger.enter("MediaStoreDS", "groupByMonth", "count" to items.size)
        val result = items.groupBy { item ->
            val dateTime = LocalDateTime.ofInstant(
                Instant.ofEpochSecond(item.dateAdded),
                ZoneId.systemDefault()
            )
            YearMonth.of(dateTime.year, dateTime.monthValue)
        }
            .map { (yearMonth, monthItems) ->
                val label = "${yearMonth.year}年${yearMonth.monthValue}月"
                com.gallery.cleaner.domain.model.MediaGroup(
                    yearMonth = yearMonth,
                    label = label,
                    mediaItems = monthItems,
                    thumbnail = monthItems.firstOrNull()
                )
            }
            .sortedByDescending { it.yearMonth }
        AppLogger.exit("MediaStoreDS", "groupByMonth", "groups=${result.size}")
        return result
    }

    suspend fun getMediaItemById(id: Long): MediaItem? {
        AppLogger.enter("MediaStoreDS", "getMediaItemById", "id" to id)
        val imageResult = queryImageById(id)
        if (imageResult != null) {
            AppLogger.exit("MediaStoreDS", "getMediaItemById", "found=image")
            return imageResult
        }

        val videoResult = queryVideoById(id)
        if (videoResult != null) {
            AppLogger.exit("MediaStoreDS", "getMediaItemById", "found=video")
            return videoResult
        }

        AppLogger.w("MediaStoreDS", "MediaItem not found: id=$id")
        return null
    }

    private fun queryImageById(id: Long): MediaItem? {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.MediaColumns.DATE_TAKEN,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT
        )

        val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

        return context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val itemId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                val mimeType = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE))
                    ?: return@use null
                val itemUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, itemId)
                val dateTakenIdx = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_TAKEN)
                MediaItem(
                    id = itemId,
                    uri = itemUri,
                    name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)) ?: "",
                    mimeType = mimeType,
                    dateAdded = resolveDisplayTimestampSeconds(
                        uri = itemUri,
                        dateTakenMillis = if (dateTakenIdx >= 0 && !cursor.isNull(dateTakenIdx)) cursor.getLong(dateTakenIdx) else null,
                        dateAddedSeconds = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)),
                        isVideo = false
                    ),
                    size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)),
                    width = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)),
                    height = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)),
                    duration = null,
                    livePhotoInfo = LivePhotoDetector.detect(context, itemUri)
                )
            } else {
                null
            }
        }
    }

    private fun queryVideoById(id: Long): MediaItem? {
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.MediaColumns.DATE_TAKEN,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.DURATION
        )

        val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)

        return context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val itemId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID))
                val mimeType = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE))
                    ?: return@use null
                val itemUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, itemId)
                val durationIdx = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
                val duration = if (durationIdx >= 0 && !cursor.isNull(durationIdx)) {
                    cursor.getLong(durationIdx)
                } else {
                    null
                }
                val dateTakenIdx = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_TAKEN)
                MediaItem(
                    id = itemId,
                    uri = itemUri,
                    name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)) ?: "",
                    mimeType = mimeType,
                    dateAdded = resolveDisplayTimestampSeconds(
                        uri = itemUri,
                        dateTakenMillis = if (dateTakenIdx >= 0 && !cursor.isNull(dateTakenIdx)) cursor.getLong(dateTakenIdx) else null,
                        dateAddedSeconds = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)),
                        isVideo = true
                    ),
                    size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)),
                    width = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)),
                    height = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)),
                    duration = duration,
                    livePhotoInfo = LivePhotoInfo()
                )
            } else {
                null
            }
        }
    }

    /**
     * 查询已删除（回收站）的媒体文件
     */
    fun queryTrashedMedia(): List<MediaItem> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            AppLogger.w("MediaStoreDS", "系统版本低于 Android R，不支持回收站查询")
            return emptyList()
        }

        AppLogger.enter("MediaStoreDS", "queryTrashedMedia")
        AppLogger.i("MediaStoreDS", "SDK_INT=${Build.VERSION.SDK_INT}, MANUFACTURER=${Build.MANUFACTURER}, MODEL=${Build.MODEL}")
        val items = mutableListOf<MediaItem>()

        try {
            queryTrashedImages(items)
        } catch (e: Exception) {
            AppLogger.exception("MediaStoreDS", "查询回收站图片失败", e)
        }

        try {
            queryTrashedVideos(items)
        } catch (e: Exception) {
            AppLogger.exception("MediaStoreDS", "查询回收站视频失败", e)
        }

        AppLogger.i("MediaStoreDS", "回收站查询完成，共 ${items.size} 项")
        AppLogger.exit("MediaStoreDS", "queryTrashedMedia", "count=${items.size}")
        return items.sortedByDescending { it.dateAdded }
    }

    private fun queryTrashedImages(items: MutableList<MediaItem>) {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT
        )

        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        AppLogger.i("MediaStoreDS", "查询回收站图片，URI: $collection")

        val selection1 = "${MediaStore.Images.Media.IS_TRASHED} = ?"
        val selectionArgs1 = arrayOf("1")
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        val queryArgs1 = Bundle().apply {
            putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection1)
            putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs1)
            putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, sortOrder)
            putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, 1)
        }
        AppLogger.i("MediaStoreDS", "IS_TRASHED=1 图片查询，QUERY_ARG_MATCH_TRASHED=1")

        val cursor1 = context.contentResolver.query(
            collection,
            projection,
            queryArgs1,
            null
        )

        if (cursor1 == null) {
            AppLogger.w("MediaStoreDS", "IS_TRASHED=1 图片查询返回 null cursor，尝试传统查询方式")
            val fallbackCursor = context.contentResolver.query(
                collection,
                projection,
                selection1,
                selectionArgs1,
                sortOrder
            )
            if (fallbackCursor == null) {
                AppLogger.e("MediaStoreDS", "传统查询方式也返回 null cursor")
            } else {
                fallbackCursor.use { cursor ->
                    AppLogger.i("MediaStoreDS", "传统方式 IS_TRASHED=1 图片 cursor 行数: ${cursor.count}")
                    processImageCursor(cursor, collection, items)
                }
            }
        } else {
            cursor1.use { cursor ->
                AppLogger.i("MediaStoreDS", "IS_TRASHED=1 图片 cursor 行数: ${cursor.count}")
                processImageCursor(cursor, collection, items)
            }
        }

        val isTrashedCount = items.size
        AppLogger.i("MediaStoreDS", "IS_TRASHED=1 图片查询完成: $isTrashedCount 张")

        val selection2 = "${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?"
        val selectionArgs2 = arrayOf(".trashed-%")

        val queryArgs2 = Bundle().apply {
            putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection2)
            putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs2)
            putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, sortOrder)
            putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, 1)
        }
        AppLogger.i("MediaStoreDS", ".trashed- 图片查询，QUERY_ARG_MATCH_TRASHED=1")

        val cursor2 = context.contentResolver.query(
            collection,
            projection,
            queryArgs2,
            null
        )

        if (cursor2 == null) {
            AppLogger.w("MediaStoreDS", ".trashed- 图片查询返回 null cursor，尝试传统查询方式")
            val fallbackCursor = context.contentResolver.query(
                collection,
                projection,
                selection2,
                selectionArgs2,
                sortOrder
            )
            if (fallbackCursor == null) {
                AppLogger.e("MediaStoreDS", "传统 .trashed- 查询也返回 null cursor")
            } else {
                fallbackCursor.use { cursor ->
                    AppLogger.i("MediaStoreDS", "传统方式 .trashed- 图片 cursor 行数: ${cursor.count}")
                    processImageCursor(cursor, collection, items, skipDuplicates = true)
                }
            }
        } else {
            cursor2.use { cursor ->
                AppLogger.i("MediaStoreDS", ".trashed- 图片 cursor 行数: ${cursor.count}")
                processImageCursor(cursor, collection, items, skipDuplicates = true)
            }
        }

        AppLogger.i("MediaStoreDS", "回收站图片查询完成: ${items.size} 张 (IS_TRASHED=$isTrashedCount, .trashed-=${items.size - isTrashedCount})")
    }

    private fun processImageCursor(
        cursor: android.database.Cursor,
        collection: android.net.Uri,
        items: MutableList<MediaItem>,
        skipDuplicates: Boolean = false
    ) {
        val idIdx = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        val nameIdx = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
        val mimeIdx = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
        val dateIdx = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
        val sizeIdx = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
        val widthIdx = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
        val heightIdx = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)

        while (cursor.moveToNext()) {
            try {
                val id = cursor.getLong(idIdx)
                if (skipDuplicates && items.none { it.id == id }) {
                    val uri = ContentUris.withAppendedId(collection, id)
                    val name = cursor.getString(nameIdx) ?: ""
                    AppLogger.d("MediaStoreDS", "发现 .trashed- 图片: id=$id, name=$name")
                    items.add(
                        MediaItem(
                            id = id,
                            uri = uri,
                            name = name,
                            mimeType = cursor.getString(mimeIdx) ?: "image/*",
                            dateAdded = cursor.getLong(dateIdx),
                            size = cursor.getLong(sizeIdx),
                            width = cursor.getInt(widthIdx),
                            height = cursor.getInt(heightIdx),
                            duration = null,
                            livePhotoInfo = LivePhotoInfo()
                        )
                    )
                } else if (!skipDuplicates) {
                    val uri = ContentUris.withAppendedId(collection, id)
                    items.add(
                        MediaItem(
                            id = id,
                            uri = uri,
                            name = cursor.getString(nameIdx) ?: "",
                            mimeType = cursor.getString(mimeIdx) ?: "image/*",
                            dateAdded = cursor.getLong(dateIdx),
                            size = cursor.getLong(sizeIdx),
                            width = cursor.getInt(widthIdx),
                            height = cursor.getInt(heightIdx),
                            duration = null,
                            livePhotoInfo = LivePhotoInfo()
                        )
                    )
                }
            } catch (e: Exception) {
                AppLogger.w("MediaStoreDS", "Error reading trashed image: ${e.message}")
            }
        }
    }

    private fun queryTrashedVideos(items: MutableList<MediaItem>) {
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.DURATION
        )

        val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        AppLogger.i("MediaStoreDS", "查询回收站视频，URI: $collection")

        val selection1 = "${MediaStore.Video.Media.IS_TRASHED} = ?"
        val selectionArgs1 = arrayOf("1")
        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        val queryArgs1 = Bundle().apply {
            putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection1)
            putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs1)
            putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, sortOrder)
            putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, 1)
        }
        AppLogger.i("MediaStoreDS", "IS_TRASHED=1 视频查询，QUERY_ARG_MATCH_TRASHED=1")

        val cursor1 = context.contentResolver.query(
            collection,
            projection,
            queryArgs1,
            null
        )

        if (cursor1 == null) {
            AppLogger.w("MediaStoreDS", "IS_TRASHED=1 视频查询返回 null cursor，尝试传统查询方式")
            val fallbackCursor = context.contentResolver.query(
                collection,
                projection,
                selection1,
                selectionArgs1,
                sortOrder
            )
            if (fallbackCursor == null) {
                AppLogger.e("MediaStoreDS", "传统视频查询方式也返回 null cursor")
            } else {
                fallbackCursor.use { cursor ->
                    AppLogger.i("MediaStoreDS", "传统方式 IS_TRASHED=1 视频 cursor 行数: ${cursor.count}")
                    processVideoCursor(cursor, collection, items)
                }
            }
        } else {
            cursor1.use { cursor ->
                AppLogger.i("MediaStoreDS", "IS_TRASHED=1 视频 cursor 行数: ${cursor.count}")
                processVideoCursor(cursor, collection, items)
            }
        }

        val isTrashedCount = items.size
        AppLogger.i("MediaStoreDS", "IS_TRASHED=1 视频查询完成: $isTrashedCount 个")

        val selection2 = "${MediaStore.Video.Media.DISPLAY_NAME} LIKE ?"
        val selectionArgs2 = arrayOf(".trashed-%")

        val queryArgs2 = Bundle().apply {
            putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection2)
            putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs2)
            putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, sortOrder)
            putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, 1)
        }
        AppLogger.i("MediaStoreDS", ".trashed- 视频查询，QUERY_ARG_MATCH_TRASHED=1")

        val cursor2 = context.contentResolver.query(
            collection,
            projection,
            queryArgs2,
            null
        )

        if (cursor2 == null) {
            AppLogger.w("MediaStoreDS", ".trashed- 视频查询返回 null cursor，尝试传统查询方式")
            val fallbackCursor = context.contentResolver.query(
                collection,
                projection,
                selection2,
                selectionArgs2,
                sortOrder
            )
            if (fallbackCursor == null) {
                AppLogger.e("MediaStoreDS", "传统 .trashed- 视频查询也返回 null cursor")
            } else {
                fallbackCursor.use { cursor ->
                    AppLogger.i("MediaStoreDS", "传统方式 .trashed- 视频 cursor 行数: ${cursor.count}")
                    processVideoCursor(cursor, collection, items, skipDuplicates = true)
                }
            }
        } else {
            cursor2.use { cursor ->
                AppLogger.i("MediaStoreDS", ".trashed- 视频 cursor 行数: ${cursor.count}")
                processVideoCursor(cursor, collection, items, skipDuplicates = true)
            }
        }

        AppLogger.i("MediaStoreDS", "回收站视频查询完成: ${items.size} 个 (IS_TRASHED=$isTrashedCount, .trashed-=${items.size - isTrashedCount})")
    }

    private fun processVideoCursor(
        cursor: android.database.Cursor,
        collection: android.net.Uri,
        items: MutableList<MediaItem>,
        skipDuplicates: Boolean = false
    ) {
        val idIdx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
        val nameIdx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
        val mimeIdx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
        val dateIdx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
        val sizeIdx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
        val widthIdx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
        val heightIdx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
        val durationIdx = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)

        while (cursor.moveToNext()) {
            try {
                val id = cursor.getLong(idIdx)
                val duration = if (durationIdx >= 0 && !cursor.isNull(durationIdx)) {
                    cursor.getLong(durationIdx)
                } else null

                if (skipDuplicates && items.none { it.id == id }) {
                    val uri = ContentUris.withAppendedId(collection, id)
                    val name = cursor.getString(nameIdx) ?: ""
                    AppLogger.d("MediaStoreDS", "发现 .trashed- 视频: id=$id, name=$name")
                    items.add(
                        MediaItem(
                            id = id,
                            uri = uri,
                            name = name,
                            mimeType = cursor.getString(mimeIdx) ?: "video/*",
                            dateAdded = cursor.getLong(dateIdx),
                            size = cursor.getLong(sizeIdx),
                            width = cursor.getInt(widthIdx),
                            height = cursor.getInt(heightIdx),
                            duration = duration,
                            livePhotoInfo = LivePhotoInfo()
                        )
                    )
                } else if (!skipDuplicates) {
                    val uri = ContentUris.withAppendedId(collection, id)
                    items.add(
                        MediaItem(
                            id = id,
                            uri = uri,
                            name = cursor.getString(nameIdx) ?: "",
                            mimeType = cursor.getString(mimeIdx) ?: "video/*",
                            dateAdded = cursor.getLong(dateIdx),
                            size = cursor.getLong(sizeIdx),
                            width = cursor.getInt(widthIdx),
                            height = cursor.getInt(heightIdx),
                            duration = duration,
                            livePhotoInfo = LivePhotoInfo()
                        )
                    )
                }
            } catch (e: Exception) {
                AppLogger.w("MediaStoreDS", "Error reading trashed video: ${e.message}")
            }
        }
    }

    private fun resolveDisplayTimestampSeconds(
        uri: android.net.Uri,
        dateTakenMillis: Long?,
        dateAddedSeconds: Long,
        isVideo: Boolean
    ): Long {
        dateTakenMillis?.takeIf { it > 0L }?.let { return it / 1000L }
        if (!isVideo) {
            readExifTimestampSeconds(uri)?.let { return it }
        }
        return dateAddedSeconds
    }

    private fun readExifTimestampSeconds(uri: android.net.Uri): Long? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)
                parseExifTimestamp(exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL))
                    ?: parseExifTimestamp(exif.getAttribute(ExifInterface.TAG_DATETIME_DIGITIZED))
                    ?: parseExifTimestamp(exif.getAttribute(ExifInterface.TAG_DATETIME))
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseExifTimestamp(value: String?): Long? {
        if (value.isNullOrBlank()) return null
        return try {
            val formatter = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getDefault()
            }
            formatter.parse(value)?.time?.div(1000L)
        } catch (_: Exception) {
            null
        }
    }
}
