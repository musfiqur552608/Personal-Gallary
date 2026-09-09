package com.freedu.personalgallary.util

import com.freedu.personalgallary.data.model.MediaItem
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Pure on-device statistics: heatmap, streaks, monthly/yearly rollups. */
object StatsUtils {

    data class DayCount(val key: String, val label: String, val count: Int)
    data class MonthCount(val key: String, val label: String, val count: Int)

    private val dayFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val dayLabel = SimpleDateFormat("d MMM", Locale.getDefault())
    private val monthKeyFmt = SimpleDateFormat("yyyy-MM", Locale.US)
    private val monthLabelFmt = SimpleDateFormat("MMM", Locale.getDefault())

    fun dayKey(ts: Long): String = synchronized(dayFmt) { dayFmt.format(Date(ts)) }

    /** Last [daysBack] days (oldest → newest), zero-filled. */
    fun heatmap(items: List<MediaItem>, daysBack: Int = 140): List<DayCount> {
        val counts = items.groupingBy { dayKey(it.dateTaken) }.eachCount()
        val cal = Calendar.getInstance()
        // align end to today
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val out = ArrayList<DayCount>(daysBack)
        for (i in daysBack - 1 downTo 0) {
            val c = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -i) }
            val key = synchronized(dayFmt) { dayFmt.format(c.time) }
            out += DayCount(
                key = key,
                label = synchronized(dayLabel) { dayLabel.format(c.time) },
                count = counts[key] ?: 0
            )
        }
        return out
    }

    /** Consecutive-day streak ending today (or yesterday, if today is empty so far). */
    fun currentStreak(items: List<MediaItem>): Int {
        if (items.isEmpty()) return 0
        val days = items.map { dayKey(it.dateTaken) }.toSet()
        val cal = Calendar.getInstance()
        var key = synchronized(dayFmt) { dayFmt.format(cal.time) }
        if (key !in days) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
            key = synchronized(dayFmt) { dayFmt.format(cal.time) }
            if (key !in days) return 0
        }
        var streak = 0
        while (key in days) {
            streak++
            cal.add(Calendar.DAY_OF_YEAR, -1)
            key = synchronized(dayFmt) { dayFmt.format(cal.time) }
        }
        return streak
    }

    fun longestStreak(items: List<MediaItem>): Int {
        if (items.isEmpty()) return 0
        val ordered = items.map { dayKey(it.dateTaken) }.toSortedSet().toList()
        var best = 1
        var run = 1
        val cal = Calendar.getInstance()
        for (i in 1 until ordered.size) {
            val prev = synchronized(dayFmt) { dayFmt.parse(ordered[i - 1])!! }
            val cur = synchronized(dayFmt) { dayFmt.parse(ordered[i])!! }
            cal.time = prev
            cal.add(Calendar.DAY_OF_YEAR, 1)
            val nextKey = synchronized(dayFmt) { dayFmt.format(cal.time) }
            run = if (nextKey == ordered[i]) run + 1 else 1
            if (run > best) best = run
        }
        return best
    }

    fun monthsOfYear(items: List<MediaItem>, year: Int): List<MonthCount> {
        val cal = Calendar.getInstance()
        return (0..11).map { m ->
            cal.set(year, m, 1)
            val label = synchronized(monthLabelFmt) { monthLabelFmt.format(cal.time) }
            val key = "%04d-%02d".format(year, m + 1)
            val count = items.count {
                synchronized(monthKeyFmt) { monthKeyFmt.format(Date(it.dateTaken)) } == key
            }
            MonthCount(key, label, count)
        }
    }

    fun yearOf(ts: Long): Int {
        val cal = Calendar.getInstance().apply { timeInMillis = ts }
        return cal.get(Calendar.YEAR)
    }

    fun shareText(
        total: Int, photos: Int, videos: Int,
        streak: Int, longest: Int, favs: Int
    ): String = buildString {
        appendLine("My Personal Gallary stats")
        appendLine("$total memories · $photos photos · $videos videos")
        appendLine("Current streak: $streak days (best $longest)")
        appendLine("$favs favorites · 100% offline, 100% mine")
    }
}
