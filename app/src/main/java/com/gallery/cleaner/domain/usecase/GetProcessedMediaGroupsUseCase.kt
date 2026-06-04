package com.gallery.cleaner.domain.usecase

import com.gallery.cleaner.domain.model.MediaGroup
import com.gallery.cleaner.domain.repository.MediaRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetProcessedMediaGroupsUseCase @Inject constructor(
    private val repository: MediaRepository
) {
    operator fun invoke(): Flow<List<MediaGroup>> = repository.getProcessedMediaGroups()
}