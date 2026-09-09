package com.freedu.personalgallary.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.freedu.personalgallary.R

/** Home-screen "memory" widget hook (stretch goal). Updates with a rotating memory photo. */
class MemoryWidgetReceiver : AppWidgetProvider() {
    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_memory)
            views.setTextViewText(R.id.widget_title, "Personal Gallary · Memories")
            mgr.updateAppWidget(id, views)
        }
    }
}
