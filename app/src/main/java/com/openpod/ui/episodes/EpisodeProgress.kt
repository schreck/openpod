package com.openpod.ui.episodes

/**
 * Parses an RSS duration string to milliseconds.
 * Accepts "H:MM:SS", "MM:SS", or plain seconds (e.g. "3600").
 * Returns null if the string is blank or unparseable.
 */
fun parseDurationMs(duration: String?): Long? {
    if (duration.isNullOrBlank()) return null
    val parts = duration.trim().split(":")
    return when (parts.size) {
        3 -> parts[0].toLongOrNull()?.let { h ->
            parts[1].toLongOrNull()?.let { m ->
                parts[2].toLongOrNull()?.let { s -> (h * 3600 + m * 60 + s) * 1000 }
            }
        }
        2 -> parts[0].toLongOrNull()?.let { m ->
            parts[1].toLongOrNull()?.let { s -> (m * 60 + s) * 1000 }
        }
        1 -> parts[0].toLongOrNull()?.let { it * 1000 }
        else -> null
    }
}
