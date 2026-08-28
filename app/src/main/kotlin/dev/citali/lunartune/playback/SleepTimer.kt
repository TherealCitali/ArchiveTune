/*
 * LunarTune (2026)
 * © cognitiveshadows03 — github.com/cognitiveshadows03
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.citali.lunartune.playback

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes

class SleepTimer(
    private val scope: CoroutineScope,
    val player: Player,
    private val service: MusicService,
) : Player.Listener {
    private var sleepTimerJob: Job? = null
    var triggerTime by mutableStateOf(-1L)
        private set
    var pauseWhenSongEnd by mutableStateOf(false)
        private set
    val isActive: Boolean
        get() = triggerTime != -1L || pauseWhenSongEnd

    fun start(minute: Int) {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        if (minute == -1) {
            pauseWhenSongEnd = true
        } else {
            val durationMs = minute.minutes.inWholeMilliseconds
            triggerTime = System.currentTimeMillis() + durationMs
            sleepTimerJob =
                scope.launch {
                    val fadeMs = FADE_MS.coerceAtMost(durationMs)
                    val waitMs = (durationMs - fadeMs).coerceAtLeast(0L)
                    delay(waitMs)
                    val startVolume = player.volume
                    val startedAt = android.os.SystemClock.elapsedRealtime()
                    while (isActive) {
                        val elapsed = android.os.SystemClock.elapsedRealtime() - startedAt
                        val progress = (elapsed.toFloat() / fadeMs.toFloat()).coerceIn(0f, 1f)
                        player.volume = (startVolume * (1f - progress)).coerceAtLeast(0f)
                        if (progress >= 1f) break
                        delay(32L)
                    }
                    service.pauseFromSleepTimer()
                }
        }
    }

    fun clear() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        pauseWhenSongEnd = false
        triggerTime = -1L
    }

    override fun onMediaItemTransition(
        mediaItem: MediaItem?,
        reason: Int,
    ) {
        if (pauseWhenSongEnd) {
            service.pauseFromSleepTimer()
        }
    }

    override fun onPlaybackStateChanged(
        @Player.State playbackState: Int,
    ) {
        if (playbackState == Player.STATE_ENDED && pauseWhenSongEnd) {
            service.pauseFromSleepTimer()
        }
    }

    companion object {
        private const val FADE_MS = 30_000L
    }
}
