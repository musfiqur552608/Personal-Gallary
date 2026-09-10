package com.freedu.personalgallary.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.freedu.personalgallary.MainActivity
import com.freedu.personalgallary.R
import com.freedu.personalgallary.data.media.MediaStoreRepository
import com.freedu.personalgallary.data.prefs.SettingsRepository
import com.freedu.personalgallary.util.FormatUtils
import com.freedu.personalgallary.util.ImageEditUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.random.Random

/** Rotating memory widget: shows a real photo, tap opens the app. */
class MemoryWidgetReceiver : AppWidgetProvider() {
    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = SettingsRepository(context.applicationContext)
                val excluded = try { settings.excludedAlbums.first() } catch (_: Exception) { emptySet() }
                val items = MediaStoreRepository(context.applicationContext)
                    .scanAll(excluded).items.filter { !it.isVideo }
                val pick = items.randomOrNull(Random(System.currentTimeMillis()))
                ids.forEach { id ->
                    val views = RemoteViews(context.packageName, R.layout.widget_memory)
                    if (pick != null) {
                        val bmp = ImageEditUtils.decodeSampled(context, pick.uri, 512)
                        if (bmp != null) {
                            views.setImageViewBitmap(R.id.widget_image, bmp)
                        }
                        views.setTextViewText(
                            R.id.widget_title,
                            FormatUtils.formatShortDate(pick.dateTaken)
                        )
                    } else {
                        views.setTextViewText(R.id.widget_title, "Personal Gallary")
                    }
                    val intent = Intent(context, MainActivity::class.java)
                    val pi = PendingIntent.getActivity(
                        context, 0, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_root, pi)
                    mgr.updateAppWidget(id, views)
                }
            } catch (_: Exception) {
            }
            pending.finish()
        }
    }
}
