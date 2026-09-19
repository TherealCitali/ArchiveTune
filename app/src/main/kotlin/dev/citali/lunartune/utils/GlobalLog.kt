/*
 * LunarTune (2026)
 * © cognitiveshadows03 — github.com/cognitiveshadows03
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.citali.lunartune.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

data class LogEntry(
    val time: Long,
    val level: Int,
    val tag: String?,
    val message: String,
)

object GlobalLog {
    private const val MAX_ENTRIES = 500

    /** Minimum spacing between two snapshot emissions while something is watching the flow. */
    private const val FLUSH_INTERVAL_MS = 100L

    // ArrayDeque gives O(1) add/remove at both ends. The previous
    // `(_logs.value + entry).takeLast(MAX_ENTRIES)` allocated two new 500-element lists and
    // emitted a StateFlow value on every single log call — hundreds of times a minute during a
    // lyrics prefetch or a stream resolution — which was measurable GC pressure and UI jank for a
    // screen that is almost never open.
    private val buffer = ArrayDeque<LogEntry>(MAX_ENTRIES)
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs = _logs.asStateFlow()

    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    @Volatile
    private var dirty = false

    @Volatile
    private var lastFlushMs = 0L

    @Synchronized
    fun append(
        level: Int,
        tag: String?,
        message: String,
    ) {
        val entry = LogEntry(System.currentTimeMillis(), level, tag, message)
        if (buffer.size >= MAX_ENTRIES) {
            buffer.removeFirst()
        }
        buffer.addLast(entry)
        dirty = true

        // Snapshots are only materialised while the logcat screen is actually collecting, and at
        // most every FLUSH_INTERVAL_MS; the screen calls flush() when it opens so nothing is missed.
        if (_logs.subscriptionCount.value > 0) {
            val now = System.currentTimeMillis()
            if (now - lastFlushMs >= FLUSH_INTERVAL_MS) {
                lastFlushMs = now
                _logs.value = buffer.toList()
                dirty = false
            }
        }
    }

    @Synchronized
    fun clear() {
        buffer.clear()
        dirty = false
        _logs.value = emptyList()
    }

    /** Publishes any buffered entries immediately (used when the logcat screen becomes visible). */
    @Synchronized
    fun flush() {
        if (dirty) {
            _logs.value = buffer.toList()
            dirty = false
            lastFlushMs = System.currentTimeMillis()
        }
    }

    fun format(entry: LogEntry): String {
        val ts = timeFormat.format(Date(entry.time))
        val lvl =
            when (entry.level) {
                android.util.Log.VERBOSE -> "V"
                android.util.Log.DEBUG -> "D"
                android.util.Log.INFO -> "I"
                android.util.Log.WARN -> "W"
                android.util.Log.ERROR -> "E"
                else -> "?"
            }
        val tag = entry.tag ?: ""
        return "[$ts] $lvl/$tag: ${entry.message}"
    }
}

/** Timber Tree that forwards logs to GlobalLog */
class GlobalLogTree : Timber.DebugTree() {
    override fun log(
        priority: Int,
        tag: String?,
        message: String,
        t: Throwable?,
    ) {
        try {
            val final = if (t != null) "$message\n$t" else message
            GlobalLog.append(priority, tag, final)
        } catch (_: Exception) {
            // swallow
        }
    }
}
