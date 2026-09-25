/*
 * LunarTune (2026)
 * © cognitiveshadows03 — github.com/cognitiveshadows03
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.citali.lunartune.ui.player

import android.media.audiofx.Visualizer
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import dev.citali.lunartune.constants.DisableAnimationsKey
import dev.citali.lunartune.playback.PlayerConnection
import dev.citali.lunartune.utils.rememberPreference
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.sqrt

/**
 * Minimal audio-reactive side lights for the expanded player ("Side Flutter"):
 * one glowing pill bar hugs each screen edge and flutters in height and
 * brightness with the music's energy. Transparent overlay — never intercepts
 * touches.
 *
 * Bar colors come from the artwork gradient (left and right edges take the
 * first and second gradient colors), falling back to the theme primary.
 * No permission needed: capture attaches to the player's own audio session.
 * Renders nothing while paused (energies decay out), when the session is
 * unavailable, or when animations are disabled.
 */
@Composable
fun FlutterLightVisualizer(
    playerConnection: PlayerConnection,
    isPlaying: Boolean,
    edgeColors: List<Color>,
    modifier: Modifier = Modifier,
) {
    val (animationsDisabled) = rememberPreference(DisableAnimationsKey, defaultValue = false)
    val energies by rememberFlutterLightEnergies(
        playerConnection = playerConnection,
        active = isPlaying && !animationsDisabled,
    )
    if (energies.all { it < FlutterLightVisibilityFloor }) {
        return
    }
    val fallback = MaterialTheme.colorScheme.primary
    val leftColor = edgeColors.getOrElse(0) { fallback }
    val rightColor = edgeColors.getOrElse(1) { leftColor }
    Canvas(modifier = modifier.fillMaxSize()) {
        drawFlutterLights(energies = energies, leftColor = leftColor, rightColor = rightColor)
    }
}

/** Below this energy on every band the overlay skips composition entirely. */
private const val FlutterLightVisibilityFloor = 0.01f

/** Energy smoothing per tick: fast attack so hits land, slow release so light lingers. */
private const val FlutterLightAttack = 0.55f
private const val FlutterLightRelease = 0.18f

private const val FlutterLightTickMs = 33L

/** FFT magnitudes below this are treated as silence. */
private const val FlutterLightNoiseFloor = 0.03f

@Composable
private fun rememberFlutterLightEnergies(
    playerConnection: PlayerConnection,
    active: Boolean,
): State<FloatArray> {
    val energies = remember { mutableStateOf(FloatArray(3)) }
    val targets = remember { FloatArray(3) }
    var sessionId by remember { mutableIntStateOf(0) }

    LaunchedEffect(active, playerConnection) {
        if (active) {
            sessionId = runCatching { playerConnection.localPlayer.audioSessionId }.getOrDefault(0)
        }
    }

    DisposableEffect(sessionId, active) {
        val visualizer =
            if (active && sessionId != 0) {
                createFlutterLightVisualizer(sessionId) { fft ->
                    val bands = fftBandEnergies(fft)
                    targets[0] = bands[0]
                    targets[1] = bands[1]
                    targets[2] = bands[2]
                }
            } else {
                targets[0] = 0f
                targets[1] = 0f
                targets[2] = 0f
                null
            }
        onDispose {
            runCatching { visualizer?.release() }
        }
    }

    LaunchedEffect(Unit) {
        val smooth = FloatArray(3)
        while (isActive) {
            var changed = false
            for (i in 0..2) {
                val target = if (active) targets[i] else 0f
                val current = smooth[i]
                val next = current + (target - current) * if (target > current) FlutterLightAttack else FlutterLightRelease
                if (next != current) {
                    smooth[i] = next
                    changed = true
                }
            }
            if (changed) {
                energies.value = smooth.copyOf()
            }
            delay(FlutterLightTickMs)
        }
    }

    return energies
}

private fun createFlutterLightVisualizer(
    sessionId: Int,
    onFft: (ByteArray) -> Unit,
): Visualizer? {
    return try {
        Visualizer(sessionId).apply {
            captureSize = Visualizer.getCaptureSizeRange().last()
            setDataCaptureListener(
                object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(
                        visualizer: Visualizer?,
                        waveform: ByteArray?,
                        samplingRate: Int,
                    ) = Unit

                    override fun onFftDataCapture(
                        visualizer: Visualizer?,
                        fft: ByteArray?,
                        samplingRate: Int,
                    ) {
                        if (fft != null) onFft(fft)
                    }
                },
                Visualizer.getMaxCaptureRate(),
                false,
                true,
            )
            enabled = true
        }
    } catch (e: Exception) {
        null
    }
}

/**
 * Collapses raw FFT bytes (interleaved real/imaginary pairs) into
 * bass/mid/treble energies in 0..1. Bin math assumes the max capture size
 * (1024 samples -> 512 bins at ~43Hz each on 44.1kHz output); bin 0 (DC)
 * is skipped.
 */
private fun fftBandEnergies(fft: ByteArray): FloatArray {
    val bins = fft.size / 2
    fun bandAverage(fromBin: Int, toBin: Int): Float {
        val last = toBin.coerceAtMost(bins - 1)
        if (fromBin > last) return 0f
        var sum = 0f
        for (bin in fromBin..last) {
            val real = fft[bin * 2].toInt()
            val imaginary = fft[bin * 2 + 1].toInt()
            sum += sqrt((real * real + imaginary * imaginary).toFloat()) / 128f
        }
        return (sum / (last - fromBin + 1)).coerceIn(0f, 1f)
    }
    return FloatArray(3) { index ->
        val raw =
            when (index) {
                0 -> bandAverage(2, 12)
                1 -> bandAverage(13, 60)
                else -> bandAverage(61, 200)
            }
        if (raw < FlutterLightNoiseFloor) 0f else sqrt(raw)
    }
}

private fun DrawScope.drawFlutterLights(
    energies: FloatArray,
    leftColor: Color,
    rightColor: Color,
) {
    if (energies.all { it < FlutterLightVisibilityFloor }) return
    // Symmetric mono energy: bass-weighted so beats land, mids/treble keep shimmer.
    val energy =
        (energies[0] * 0.5f + energies[1] * 0.3f + energies[2] * 0.2f).coerceIn(0f, 1f)
    val centerY = size.height / 2f
    val halfHeight = size.height * (0.34f + 0.22f * energy) / 2f
    val alpha = (0.55f + 0.40f * energy).coerceIn(0f, 0.95f)
    for (side in intArrayOf(-1, 1)) {
        val color = if (side < 0) leftColor else rightColor
        val edgeX = if (side < 0) 0f else size.width
        val direction = if (side < 0) 1f else -1f
        val x = edgeX + direction * 8.dp.toPx()
        val top = Offset(x, centerY - halfHeight)
        val bottom = Offset(x, centerY + halfHeight)
        drawLine(
            color = color.copy(alpha = alpha * 0.14f),
            start = top,
            end = bottom,
            strokeWidth = 40.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color.copy(alpha = alpha * 0.35f),
            start = top,
            end = bottom,
            strokeWidth = 22.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color.copy(alpha = alpha),
            start = top,
            end = bottom,
            strokeWidth = 10.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}
