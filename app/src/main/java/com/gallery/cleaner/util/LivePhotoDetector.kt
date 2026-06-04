package com.gallery.cleaner.util

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import com.gallery.cleaner.domain.model.LivePhotoInfo
import com.gallery.cleaner.domain.model.LivePhotoType
import com.gallery.cleaner.util.log.AppLogger
import java.io.RandomAccessFile

object LivePhotoDetector {
    private const val TAG = "LivePhotoDetector"

    fun detect(context: Context, uri: Uri): LivePhotoInfo {
        // 优先尝试文件路径方式（兼容旧设备）
        val filePath = uri.toFilePath(context)
        AppLogger.i(TAG, "detect: uri=$uri, filePath=$filePath")

        // 1. XMP 元数据检测
        val xmpResult = if (filePath != null) {
            detectByXMP(filePath)
        } else {
            detectByXMPFromUri(context, uri)
        }
        if (xmpResult != null) {
            AppLogger.i(TAG, "detect: XMP 检测命中, type=${xmpResult.type}, offset=${xmpResult.videoOffset}, length=${xmpResult.videoLength}")
            return xmpResult
        }

        // 2. 文件末尾标识检测
        val footerResult = if (filePath != null) {
            detectByFooter(filePath)
        } else {
            detectByFooterFromUri(context, uri)
        }
        if (footerResult != null) {
            AppLogger.i(TAG, "detect: Footer 检测命中, type=${footerResult.type}")
            return footerResult
        }

        // 3. 配对视频文件检测（vivo / Apple）
        val pairedResult = detectByPairedVideo(context, uri)
        if (pairedResult != null) {
            AppLogger.i(TAG, "detect: 配对视频检测命中, type=${pairedResult.type}, pairedUri=${pairedResult.pairedVideoUri}")
            return pairedResult
        }

        // 4. 视频签名回退检测
        if (filePath != null && containsVideoSignature(filePath)) {
            val info = LivePhotoInfo(
                type = LivePhotoType.GOOGLE_PIXEL,
                videoOffset = estimateVideoOffset(filePath)
            )
            AppLogger.i(TAG, "detect: 视频签名回退命中, offset=${info.videoOffset}")
            return info
        }

        AppLogger.d(TAG, "detect: 非 Live Photo, uri=$uri")
        return LivePhotoInfo()
    }

    // ==================== XMP 检测 ====================

    private fun detectByXMP(filePath: String): LivePhotoInfo? {
        return try {
            val exif = ExifInterface(filePath)
            val xmpString = exif.getAttribute(ExifInterface.TAG_XMP) ?: return null
            parseXmp(xmpString)
        } catch (e: Exception) {
            null
        }
    }

    private fun detectByXMPFromUri(context: Context, uri: Uri): LivePhotoInfo? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val exif = ExifInterface(input)
                val xmpString = exif.getAttribute(ExifInterface.TAG_XMP) ?: return null
                parseXmp(xmpString)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseXmp(xmpString: String): LivePhotoInfo? {
        return when {
            xmpString.contains("GCamera:MicroVideo=\"1\"") ||
            xmpString.contains("GCamera:MicroVideo=\"true\"") -> {
                val offset = extractXmpValue(xmpString, "GCamera:MicroVideoOffset")
                val length = extractXmpValue(xmpString, "GCamera:MicroVideoPresentationTimestamp")
                LivePhotoInfo(
                    type = LivePhotoType.XIAOMI,
                    videoOffset = offset,
                    videoLength = length
                )
            }
            xmpString.contains("GCamera:MotionPhoto=\"1\"") ||
            xmpString.contains("GCamera:MotionPhoto=\"true\"") -> {
                val offset = extractXmpValue(xmpString, "GCamera:MotionPhotoPresentationTimestamp")
                LivePhotoInfo(
                    type = LivePhotoType.GOOGLE_PIXEL,
                    videoOffset = offset
                )
            }
            xmpString.contains("OpCamera:MotionPhotoOwner=\"oplus\"") -> {
                LivePhotoInfo(type = LivePhotoType.OPPO)
            }
            else -> null
        }
    }

    // ==================== 文件末尾检测 ====================

    private fun detectByFooter(filePath: String): LivePhotoInfo? {
        return try {
            RandomAccessFile(filePath, "r").use { raf ->
                val fileLength = raf.length()
                val readSize = minOf(2048, fileLength.toInt())
                if (readSize <= 0) return null
                raf.seek(fileLength - readSize)
                val buffer = ByteArray(readSize)
                raf.readFully(buffer)
                val footer = String(buffer, Charsets.UTF_8)
                parseFooter(footer)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun detectByFooterFromUri(context: Context, uri: Uri): LivePhotoInfo? {
        return try {
            // 先获取文件大小
            val fileLength = getFileLength(context, uri)
            if (fileLength <= 0) return null

            context.contentResolver.openInputStream(uri)?.use { input ->
                // 跳过到文件末尾 2048 字节处
                val skipBytes = fileLength - minOf(2048, fileLength.toInt())
                if (skipBytes > 0) {
                    input.skip(skipBytes)
                }
                val buffer = ByteArray(minOf(2048, fileLength.toInt()))
                val read = input.read(buffer)
                if (read > 0) {
                    val footer = String(buffer, 0, read, Charsets.UTF_8)
                    parseFooter(footer)
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseFooter(footer: String): LivePhotoInfo? {
        return when {
            footer.contains("vivo{") -> {
                LivePhotoInfo(type = LivePhotoType.VIVO)
            }
            footer.contains("LIVE_") -> {
                val videoLength = parseHuaweiVideoLength(footer)
                LivePhotoInfo(
                    type = LivePhotoType.HUAWEI,
                    videoLength = videoLength
                )
            }
            else -> null
        }
    }

    // ==================== 配对视频检测 ====================

    private fun detectByPairedVideo(context: Context, imageUri: Uri): LivePhotoInfo? {
        // 获取图片的 DISPLAY_NAME
        val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)
        val cursor = try {
            context.contentResolver.query(imageUri, projection, null, null, null)
        } catch (e: Exception) { null }

        val baseName = cursor?.use {
            if (it.moveToFirst()) {
                val idx = it.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                it.getString(idx)?.substringBeforeLast(".")
            } else null
        } ?: return null

        // 获取图片所在目录的 URI
        val parentUri = imageUri.parentUri() ?: return null

        // 查找同目录下的 .mp4 (vivo) 和 .mov (Apple)
        val pairedVideoUri = findPairedVideo(context, imageUri, parentUri, baseName)
        return when {
            pairedVideoUri?.second == "mp4" -> LivePhotoInfo(
                type = LivePhotoType.VIVO,
                pairedVideoUri = pairedVideoUri.first
            )
            pairedVideoUri?.second == "mov" -> LivePhotoInfo(
                type = LivePhotoType.APPLE,
                pairedVideoUri = pairedVideoUri.first
            )
            else -> null
        }
    }

    private fun findPairedVideo(
        context: Context,
        imageUri: Uri,
        @Suppress("UNUSED_PARAMETER") parentUri: Uri,
        baseName: String
    ): Pair<Uri, String>? {
        // 使用 ContentResolver 查询，通过 RELATIVE_PATH 过滤同目录
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME
        )

        // 获取图片的 RELATIVE_PATH
        val imageRelPath = try {
            context.contentResolver.query(
                imageUri,
                arrayOf(MediaStore.MediaColumns.RELATIVE_PATH),
                null, null, null
            )?.use {
                if (it.moveToFirst()) {
                    val idx = it.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
                    if (idx >= 0 && !it.isNull(idx)) it.getString(idx) else null
                } else null
            }
        } catch (e: Exception) { null }

        // 在 MediaStore 中查询匹配的视频文件
        val collection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Files.getContentUri("external")
        }

        val videoSelectionArgs = if (imageRelPath != null) {
            arrayOf("$baseName.mp4", "$baseName.mov", imageRelPath)
        } else {
            arrayOf("$baseName.mp4", "$baseName.mov")
        }

        val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} IN (?, ?)" +
            if (imageRelPath != null) " AND ${MediaStore.MediaColumns.RELATIVE_PATH} = ?" else ""
        return try {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                videoSelectionArgs,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idIdx = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                    val nameIdx = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                    val id = cursor.getLong(idIdx)
                    val name = cursor.getString(nameIdx)
                    val ext = name.substringAfterLast(".")
                    val contentUri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
                    Pair(android.content.ContentUris.withAppendedId(contentUri, id), ext)
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    // ==================== 工具方法 ====================

    private fun containsVideoSignature(filePath: String): Boolean {
        return try {
            RandomAccessFile(filePath, "r").use { raf ->
                val buffer = ByteArray(4096)
                val read = raf.read(buffer)
                if (read > 0) {
                    val header = String(buffer, 0, read, Charsets.ISO_8859_1)
                    header.contains("ftyp") || header.contains("moov")
                } else false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun estimateVideoOffset(filePath: String): Long {
        return try {
            RandomAccessFile(filePath, "r").use { raf ->
                val fileLength = raf.length()
                val buffer = ByteArray(minOf(65536, fileLength.toInt()))
                raf.read(buffer)
                val header = String(buffer, Charsets.ISO_8859_1)
                val ftypIndex = header.indexOf("ftyp")
                if (ftypIndex >= 0) ftypIndex.toLong() else 0L
            }
        } catch (e: Exception) {
            0L
        }
    }

    private fun extractXmpValue(xmp: String, key: String): Long {
        val regex = "$key=\"(\\d+)\"".toRegex()
        return regex.find(xmp)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
    }

    private fun parseHuaweiVideoLength(footer: String): Long {
        val regex = "LIVE_(\\d+)".toRegex()
        return regex.find(footer)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
    }

    private fun getFileLength(context: Context, uri: Uri): Long {
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { fd ->
                fd.statSize
            } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    private fun Uri.toFilePath(context: Context): String? {
        return when (scheme) {
            "file" -> path
            "content" -> {
                val projection = arrayOf(MediaStore.MediaColumns.DATA)
                context.contentResolver.query(this, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
                        cursor.getString(idx)
                    } else null
                }
            }
            else -> null
        }
    }

    private fun Uri.parentUri(): Uri? {
        val path = this.path ?: return null
        val parentPath = path.substringBeforeLast("/")
        if (parentPath.isEmpty() || parentPath == path) return null
        return this.buildUpon().path(parentPath).build()
    }
}