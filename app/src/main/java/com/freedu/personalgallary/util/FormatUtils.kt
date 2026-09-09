package com.freedu.personalgallary.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object FormatUtils {
    fun formatDuration(ms: Long): String {
        if (ms <= 0) return "0:00"
        val totalSec = TimeUnit.MILLISECONDS.toSeconds(ms)
        val m = totalSec / 60
        val s = totalSec % 60
        return if (m >= 60) {
            "%d:%02d:%02d".format(m / 60, m % 60, s)
        } else {
            "%d:%02d".format(m, s)
        }
    }

    fun formatDate(ts: Long): String {
        if (ts <= 0) return ""
        return SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault()).format(Date(ts))
    }

    fun formatShortDate(ts: Long): String {
        if (ts <= 0) return ""
        return SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(ts))
    }

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var v = bytes.toDouble()
        var i = 0
        while (v >= 1024 && i < units.lastIndex) {
            v /= 1024
            i++
        }
        return if (i == 0) "%d %s".format(bytes, units[i]) else "%.1f %s".format(v, units[i])
    }

    fun monthKey(ts: Long): String {
        return SimpleDateFormat("yyyy-MM", Locale.US).format(Date(ts))
    }

    fun monthLabel(ts: Long): String {
        return SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(ts))
    }

    fun isSameDay(a: Long, b: Long): Boolean {
        val ca = Calendar.getInstance().apply { timeInMillis = a }
        val cb = Calendar.getInstance().apply { timeInMillis = b }
        return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) &&
            ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
    }

    /** "On this day": same month+day, different year */
    fun isOnThisDay(ts: Long, now: Long = System.currentTimeMillis()): Boolean {
        val c = Calendar.getInstance().apply { timeInMillis = ts }
        val n = Calendar.getInstance().apply { timeInMillis = now }
        return c.get(Calendar.MONTH) == n.get(Calendar.MONTH) &&
            c.get(Calendar.DAY_OF_MONTH) == n.get(Calendar.DAY_OF_MONTH) &&
            c.get(Calendar.YEAR) != n.get(Calendar.YEAR)
    }
}
