package com.gallery.cleaner.util

import android.content.Context
import android.net.Uri
import com.gallery.cleaner.domain.model.LivePhotoType
import com.gallery.cleaner.domain.model.MediaItem
import com.gallery.cleaner.util.log.AppLogger
import java.io.File
import java.io.FileOutputStream

object LivePhotoExtractor {
    private const val TAG = "LivePhotoExtractor"

    fun extractVideo(context: Context, mediaItem: MediaItem, outputFile: File): Boolean {
        AppLogger.enter(TAG, "extractVideo", "type" to mediaItem.livePhotoInfo.type,
            "videoOffset" to mediaItem.livePhotoInfo.videoOffset,
            "videoLength" to mediaItem.livePhotoInfo.videoLength,
            "outputFile" to outputFile.absolutePath)

        val result = when (mediaItem.livePhotoInfo.type) {
            LivePhotoType.XIAOMI -> extractXiaomiVideo(context, mediaItem, outputFile)
            LivePhotoType.GOOGLE_PIXEL, LivePhotoType.OPPO -> extractEmbeddedVideo(context, mediaItem, outputFile)
            LivePhotoType.HUAWEI -> extractHuaweiVideo(context, mediaItem, outputFile)
            LivePhotoType.VIVO, LivePhotoType.APPLE -> copyPairedVideo(context, mediaItem, outputFile)
            else -> {
                AppLogger.w(TAG, "不支持的 Live Photo 类型: ${mediaItem.livePhotoInfo.type}")
                false
            }
        }

        AppLogger.exit(TAG, "extractVideo", "result=$result, fileSize=${if (outputFile.exists()) outputFile.length() else 0}")
        return result
    }

    private fun extractXiaomiVideo(context: Context, mediaItem: MediaItem, outputFile: File): Boolean {
        val offset = mediaItem.livePhotoInfo.videoOffset
        if (offset <= 0) {
            AppLogger.w(TAG, "Xiaomi: videoOffset=$offset 无效")
            return false
        }
        AppLogger.i(TAG, "Xiaomi: 从 offset=$offset 提取视频")
        return extractFromOffset(context, mediaItem.uri, outputFile, offset)
    }

    private fun extractEmbeddedVideo(context: Context, mediaItem: MediaItem, outputFile: File): Boolean {
        val videoLength = mediaItem.livePhotoInfo.videoLength
        val fileLength = getFileLength(context, mediaItem.uri)
        AppLogger.i(TAG, "Embedded: fileLength=$fileLength, videoLength=$videoLength")
        if (fileLength <= 0 || videoLength <= 0) {
            AppLogger.w(TAG, "Embedded: 参数无效，尝试从文件末尾提取")
            // 回退：如果 videoLength 为 0，尝试从文件末尾提取
            if (fileLength > 0) {
                // 尝试读取文件末尾，查找视频签名
                return tryExtractFromEnd(context, mediaItem.uri, outputFile, fileLength)
            }
            return false
        }
        val offset = fileLength - videoLength
        AppLogger.i(TAG, "Embedded: 从 offset=$offset 提取 $videoLength 字节")
        return extractFromOffset(context, mediaItem.uri, outputFile, offset, videoLength)
    }

    private fun extractHuaweiVideo(context: Context, mediaItem: MediaItem, outputFile: File): Boolean {
        val videoLength = mediaItem.livePhotoInfo.videoLength
        val fileLength = getFileLength(context, mediaItem.uri)
        AppLogger.i(TAG, "Huawei: fileLength=$fileLength, videoLength=$videoLength")
        if (fileLength <= 0 || videoLength <= 0) {
            AppLogger.w(TAG, "Huawei: 参数无效")
            return false
        }
        val offset = fileLength - videoLength
        return extractFromOffset(context, mediaItem.uri, outputFile, offset, videoLength)
    }

    private fun copyPairedVideo(context: Context, mediaItem: MediaItem, outputFile: File): Boolean {
        val videoUri = mediaItem.livePhotoInfo.pairedVideoUri
        if (videoUri == null) {
            AppLogger.w(TAG, "PairedVideo: pairedVideoUri 为空")
            return false
        }
        AppLogger.i(TAG, "PairedVideo: 复制 $videoUri, type=${mediaItem.livePhotoInfo.type}")
        return copyUriToFile(context, videoUri, outputFile)
    }

    /**
     * 回退方案：当 videoLength 未知时，尝试从文件末尾搜索视频签名并提取
     */
    private fun tryExtractFromEnd(context: Context, uri: Uri, outputFile: File, fileLength: Long): Boolean {
        return try {
            // 搜索最后 1MB 区域中的 ftyp 签名
            val searchRegion = minOf(1024 * 1024, fileLength)
            val searchOffset = fileLength - searchRegion

            context.contentResolver.openInputStream(uri)?.use { input ->
                if (searchOffset > 0) input.skip(searchOffset)
                val buffer = ByteArray(searchRegion.toInt())
                val read = input.read(buffer)

                if (read > 0) {
                    val content = String(buffer, 0, read, Charsets.ISO_8859_1)
                    val ftypIndex = content.indexOf("ftyp")
                    if (ftypIndex >= 0) {
                        // ftyp 前面通常有 4 字节的 box size
                        val videoStart = maxOf(0, ftypIndex - 4)
                        val absoluteOffset = searchOffset + videoStart
                        AppLogger.i(TAG, "回退提取: 在 offset=$absoluteOffset 发现 ftyp 签名")
                        return extractFromOffset(context, uri, outputFile, absoluteOffset)
                    } else {
                        AppLogger.w(TAG, "回退提取: 未在文件末尾发现视频签名")
                    }
                }
            }
            false
        } catch (e: Exception) {
            AppLogger.exception(TAG, "回退提取失败", e)
            false
        }
    }

    private fun extractFromOffset(
        context: Context,
        uri: Uri,
        outputFile: File,
        offset: Long,
        length: Long = -1
    ): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val skipped = input.skip(offset)
                if (skipped < offset) {
                    AppLogger.w(TAG, "跳过字节不足: skipped=$skipped, required=$offset")
                    return false
                }

                FileOutputStream(outputFile).use { output ->
                    if (length > 0) {
                        val buffer = ByteArray(8192)
                        var remaining = length
                        while (remaining > 0) {
                            val toRead = minOf(buffer.size, remaining.toInt())
                            val read = input.read(buffer, 0, toRead)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            remaining -= read
                        }
                    } else {
                        input.copyTo(output)
                    }
                }
            }
            val success = outputFile.exists() && outputFile.length() > 0
            AppLogger.i(TAG, "extractFromOffset: success=$success, size=${outputFile.length()}")
            success
        } catch (e: Exception) {
            AppLogger.exception(TAG, "extractFromOffset 失败: offset=$offset, length=$length", e)
            false
        }
    }

    private fun copyUriToFile(context: Context, uri: Uri, outputFile: File): Boolean {
        AppLogger.i(TAG, "copyUriToFile: uri=$uri, scheme=${uri.scheme}, authority=${uri.authority}")
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                AppLogger.e(TAG, "copyUriToFile: openInputStream 返回 null for $uri")
                return false
            }
            inputStream.use { input ->
                FileOutputStream(outputFile).use { output ->
                    input.copyTo(output)
                }
            }
            val success = outputFile.exists() && outputFile.length() > 0
            AppLogger.i(TAG, "copyUriToFile: success=$success, size=${outputFile.length()}")
            success
        } catch (e: Exception) {
            AppLogger.exception(TAG, "copyUriToFile 失败: $uri", e)
            false
        }
    }

    private fun getFileLength(context: Context, uri: Uri): Long {
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { fd ->
                fd.statSize
            } ?: 0L
        } catch (e: Exception) {
            AppLogger.w(TAG, "getFileLength 失败: $uri -> ${e.message}")
            0L
        }
    }
}