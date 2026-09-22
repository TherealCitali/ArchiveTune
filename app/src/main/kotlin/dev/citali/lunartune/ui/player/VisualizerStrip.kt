/*
 * LunarTune (2026)
 * © cognitiveshadows03 — github.com/cognitiveshadows03
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.citali.lunartune.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.citali.lunartune.audio.PlayerVisualizer
import dev.citali.lunartune.constants.VisualizerEnabledKey
import dev.citali.lunartune.utils.rememberPreference
import kotlinx.coroutines.delay

/**
 * Live spectrum strip shown between the track title and the seek bar on the expanded
 * player. Self-contained: it reads the [VisualizerEnabledKey] toggle itself and syncs
 * the [PlayerVisualizer] engine, so callers only place it. When paused no PCM flows,
 * so the bars gracefully decay to flat instead of freezing mid-dance.
 */
@Composable
fun PlayerVisualizerStrip(
    isPlaying: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val (enabled) = rememberPreference(VisualizerEnabledKey, true)

    LaunchedEffect(enabled) {
        PlayerVisualizer.setEnabled(enabled)
    }

    AnimatedVisibility(
        visible = enabled,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier,
    ) {
        val levels by PlayerVisualizer.levels.collectAsState()
        var displayLevels by remember { mutableStateOf(FloatArray(PlayerVisualizer.BAND_COUNT)) }

        LaunchedEffect(levels, isPlaying) {
            if (isPlaying) {
                displayLevels = levels
            }
        }
        LaunchedEffect(isPlaying) {
            if (!isPlaying) {
                while (true) {
                    val decayed = displayLevels.map { it * VisualizerPauseDecay }.toFloatArray()
                    if (decayed.all { it < 0.01f }) {
                        displayLevels = FloatArray(PlayerVisualizer.BAND_COUNT)
                        break
                    }
                    displayLevels = decayed
                    delay(VisualizerFrameDelayMs)
                }
            }
        }

        val brush =
            remember(color) {
                Brush.verticalGradient(listOf(color, color.copy(alpha = 0.35f)))
            }
        Canvas(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .padding(horizontal = 28.dp),
        ) {
            val snapshot = displayLevels
            if (snapshot.isEmpty()) return@Canvas
            val gap = 3.dp.toPx()
            val barWidth = 4.dp.toPx()
            val count = ((size.width + gap) / (barWidth + gap)).toInt().coerceAtLeast(8)
            val step = snapshot.size / count.toFloat()
            var x = (size.width - (count * barWidth + (count - 1) * gap)) / 2f
            val minHeight = 2.dp.toPx()
            for (i in 0 until count) {
                val from = (i * step).toInt()
                val to = ((i + 1) * step).toInt().coerceAtMost(snapshot.size)
                var peak = 0f
                for (j in from until to) {
                    if (snapshot[j] > peak) peak = snapshot[j]
                }
                val height = (minHeight + peak * (size.height - minHeight)).coerceAtMost(size.height)
                drawRoundRect(
                    brush = brush,
                    topLeft = Offset(x, size.height - height),
                    size = Size(barWidth, height),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
                )
                x += barWidth + gap
            }
        }
    }
}

private const val VisualizerPauseDecay = 0.88f
private const val VisualizerFrameDelayMs = 33L
