package com.gallery.cleaner.domain.usecase

import com.gallery.cleaner.domain.model.MediaItem
import com.gallery.cleaner.domain.repository.MediaRepository
import javax.inject.Inject

class GetMediaItemUseCase @Inject constructor(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(id: Long): MediaItem? = repository.getMediaItemById(id)
}
