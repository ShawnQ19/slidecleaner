package com.gallery.cleaner.data.repository

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gallery.cleaner.util.log.AppLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.randomCleanupDataStore: DataStore<Preferences> by preferencesDataStore(name = "random_cleanup_prefs")

@Singleton
class ProcessedMediaStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "ProcessedMediaStore"
        private val KEY_PROCESSED_URIS = stringSetPreferencesKey("processed_uris")
    }

    suspend fun getProcessedUris(): Set<String> {
        return context.randomCleanupDataStore.data.first()[KEY_PROCESSED_URIS] ?: emptySet()
    }

    fun observeProcessedUris(): Flow<Set<String>> {
        return context.randomCleanupDataStore.data.map { prefs ->
            prefs[KEY_PROCESSED_URIS] ?: emptySet()
        }
    }

    suspend fun markProcessed(uris: Collection<Uri>) {
        if (uris.isEmpty()) return
        val uriStrings = uris.map { it.toString() }.toSet()
        AppLogger.userAction(TAG, "markProcessed", "count=${uriStrings.size}")
        context.randomCleanupDataStore.edit { prefs ->
            val existing = prefs[KEY_PROCESSED_URIS] ?: emptySet()
            prefs[KEY_PROCESSED_URIS] = existing + uriStrings
        }
    }

    suspend fun unmarkProcessed(uris: Collection<Uri>) {
        if (uris.isEmpty()) return
        val uriStrings = uris.map { it.toString() }.toSet()
        AppLogger.userAction(TAG, "unmarkProcessed", "count=${uriStrings.size}")
        context.randomCleanupDataStore.edit { prefs ->
            val existing = prefs[KEY_PROCESSED_URIS] ?: emptySet()
            prefs[KEY_PROCESSED_URIS] = existing - uriStrings
        }
    }

    suspend fun clearProcessed() {
        AppLogger.userAction(TAG, "clearProcessed")
        context.randomCleanupDataStore.edit { prefs ->
            prefs.remove(KEY_PROCESSED_URIS)
        }
    }

    suspend fun processedCount(): Int {
        return getProcessedUris().size
    }
}