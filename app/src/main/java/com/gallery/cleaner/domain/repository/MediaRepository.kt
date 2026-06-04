package com.gallery.cleaner.domain.repository

import android.net.Uri
import com.gallery.cleaner.domain.model.MediaGroup
import com.gallery.cleaner.domain.model.MediaItem
import kotlinx.coroutines.flow.Flow
import java.time.YearMonth

sealed class TrashResult {
    data class Success(val deletedCount: Int) : TrashResult()
    data class Error(val message: String) : TrashResult()
}

sealed class DeleteResult {
    data class Success(val deletedCount: Int) : DeleteResult()
    data class Error(val message: String) : DeleteResult()
}

interface MediaRepository {
    fun getMediaGroups(): Flow<List<MediaGroup>>
    fun getProcessedMediaGroups(): Flow<List<MediaGroup>>
    suspend fun getMediaItemsByMonth(yearMonth: YearMonth): List<MediaItem>
    suspend fun getProcessedMediaItemsByMonth(yearMonth: YearMonth): List<MediaItem>
    suspend fun getRandomMediaItems(excludedUris: Set<String>, sampleSize: Int): List<MediaItem>
    suspend fun moveToTrash(uris: List<Uri>): TrashResult
    suspend fun restoreFromTrash(uris: List<Uri>): TrashResult
    suspend fun deletePermanently(uris: List<Uri>): DeleteResult
    fun isTrashSupported(): Boolean
    suspend fun getMediaItemById(id: Long): MediaItem?
    suspend fun getTrashedMediaItems(): List<MediaItem>
}
