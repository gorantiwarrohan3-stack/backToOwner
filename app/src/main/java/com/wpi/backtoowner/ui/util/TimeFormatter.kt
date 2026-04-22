package com.wpi.backtoowner.ui.util

import java.util.concurrent.TimeUnit

object TimeFormatter {

    fun relative(epochMs: Long): String {
        if (epochMs <= 0L) return "Just now"
        val diffMs = (System.currentTimeMillis() - epochMs).coerceAtLeast(0L)
        val mins = TimeUnit.MILLISECONDS.toMinutes(diffMs)
        if (mins < 1) return "Just now"
        if (mins < 60) return "${mins} min ago"
        val hrs = TimeUnit.MILLISECONDS.toHours(diffMs)
        if (hrs < 24) return "${hrs} hrs ago"
        val days = TimeUnit.MILLISECONDS.toDays(diffMs)
        if (days == 1L) return "Yesterday"
        if (days < 7) return "${days} days ago"
        return "${days / 7} wk ago"
    }
}
