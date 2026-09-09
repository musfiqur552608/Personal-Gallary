package com.freedu.personalgallary.worker

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.freedu.personalgallary.MainActivity
import com.freedu.personalgallary.R
import com.freedu.personalgallary.data.media.MediaStoreRepository
import com.freedu.personalgallary.data.prefs.SettingsRepository
import com.freedu.personalgallary.util.FormatUtils
import kotlinx.coroutines.flow.first

/** Daily "you have N memories today" local notification. Fully offline. */
class ReminderWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        return try {
            val settings = SettingsRepository(applicationContext)
            if (!settings.dailyReminder.first()) return Result.success()
            val excluded = try { settings.excludedAlbums.first() } catch (_: Exception) { emptySet() }
            val items = MediaStoreRepository(applicationContext).scanAll(excluded).items
            val memories = items.filter { FormatUtils.isOnThisDay(it.dateTaken) }
            if (memories.isEmpty()) return Result.success()
            val intent = Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pi = PendingIntent.getActivity(
                applicationContext, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notif = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("You have ${memories.size} memories today")
                .setContentText("Tap to revisit moments from this day in past years.")
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build()
            val mgr = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            mgr.notify(NOTIF_ID, notif)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val CHANNEL_ID = "memories"
        const val NOTIF_ID = 41
        const val WORK_NAME = "daily-memory"
    }
}
