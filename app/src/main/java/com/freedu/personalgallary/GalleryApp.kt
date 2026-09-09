package com.freedu.personalgallary

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import com.freedu.personalgallary.worker.MediaRescanWorker
import com.freedu.personalgallary.worker.ReminderWorker
import java.util.concurrent.TimeUnit

class GalleryApp : Application(), ImageLoaderFactory {

    /** Coil with video-frame support so video thumbnails render everywhere. */
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components { add(VideoFrameDecoder.Factory()) }
            .crossfade(true)
            .build()

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                ReminderWorker.CHANNEL_ID,
                "Memories",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Daily on-this-day memory reminders (offline)" }
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
        // Periodic incremental re-scan to catch new media (every 6h, cheap + local)
        val req = PeriodicWorkRequestBuilder<MediaRescanWorker>(6, TimeUnit.HOURS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "media-rescan",
            ExistingPeriodicWorkPolicy.KEEP,
            req
        )
    }
}
