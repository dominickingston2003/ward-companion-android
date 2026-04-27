package com.wardcompanion.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private val dayFmt = SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault())
private val timeFmt = SimpleDateFormat("d MMM, h:mm a", Locale.getDefault())

fun formatDay(epochMs: Long): String = dayFmt.format(Date(epochMs))
fun formatDateTime(epochMs: Long): String = timeFmt.format(Date(epochMs))

fun formatRelative(epochMs: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - epochMs
    if (diff < 0) return formatDay(epochMs)
    val mins = TimeUnit.MILLISECONDS.toMinutes(diff)
    val hrs  = TimeUnit.MILLISECONDS.toHours(diff)
    val days = TimeUnit.MILLISECONDS.toDays(diff)
    return when {
        mins < 1   -> "just now"
        mins < 60  -> "${mins}m ago"
        hrs  < 24  -> "${hrs}h ago"
        days < 7   -> "${days}d ago"
        else       -> formatDay(epochMs)
    }
}
