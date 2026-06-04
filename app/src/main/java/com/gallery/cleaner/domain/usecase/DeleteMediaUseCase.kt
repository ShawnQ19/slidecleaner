package com.gallery.cleaner.domain.usecase

import android.net.Uri
import com.gallery.cleaner.domain.repository.DeleteResult
import com.gallery.cleaner.domain.repository.MediaRepository
import com.gallery.cleaner.domain.repository.TrashResult
import javax.inject.Inject

class DeleteMediaUseCase @Inject constructor(
    val repository: MediaRepository
) {
    suspend operator fun invoke(uris: List<Uri>): TrashResult {
        return if (repository.isTrashSupported()) {
            repository.moveToTrash(uris)
        } else {
            when (val result = repository.deletePermanently(uris)) {
                is DeleteResult.Success -> TrashResult.Success(result.deletedCount)
                is DeleteResult.Error -> TrashResult.Error(result.message)
            }
        }
    }
}
