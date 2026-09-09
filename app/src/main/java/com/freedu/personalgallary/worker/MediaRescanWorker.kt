package com.freedu.personalgallary.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.freedu.personalgallary.data.media.MediaStoreRepository
import com.freedu.personalgallary.data.prefs.SettingsRepository
import kotlinx.coroutines.flow.first

/** Cheap periodic incremental re-scan trigger. Actual list is rebuilt on app resume. */
class MediaRescanWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        return try {
            val settings = SettingsRepository(applicationContext)
            val excluded = try { settings.excludedAlbums.first() } catch (_: Exception) { emptySet() }
            MediaStoreRepository(applicationContext).scanAll(excluded)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
