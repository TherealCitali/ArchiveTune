/*
 * LunarTune (2026)
 * © cognitiveshadows03 — github.com/cognitiveshadows03
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.citali.lunartune.audio

import androidx.media3.common.C
import androidx.media3.exoplayer.audio.TeeAudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Permission-free live spectrum analyzer for the player visualizer.
 *
 * A [TeeAudioProcessor] tapped into the local player's audio chain feeds PCM into this
 * sink (no microphone permission needed, unlike `android.media.audiofx.Visualizer`).
 * Samples are mixed to mono, windowed, FFT'd and reduced to [BAND_COUNT] smoothed
 * 0..1 levels exposed as [levels].
 *
 * All [TeeAudioProcessor.AudioBufferSink] callbacks arrive on the playback thread, so
 * [handleBuffer] never blocks: it copies samples, runs a fixed-size FFT and throttles
 * emissions to ~30fps. Attach only to the main player — the crossfade player has no tap.
 */
object PlayerVisualizer : TeeAudioProcessor.AudioBufferSink {
    const val BAND_COUNT = 64
    const val FFT_SIZE = 2048

    private const val EMIT_INTERVAL_MS = 33L
    private const val ATTACK = 0.6f
    private const val RELEASE = 0.88f
    private const val MIN_HZ = 60f
    private const val MAX_HZ = 16_000f

    private val _levels = MutableStateFlow(FloatArray(BAND_COUNT))
    val levels: StateFlow<FloatArray> = _levels.asStateFlow()

    /** Tee instance wired into the player's audio processor chain. */
    val teeProcessor: TeeAudioProcessor by lazy { TeeAudioProcessor(this) }

    @Volatile
    var isEnabled = true
        private set

    // Hamming window, precomputed once.
    private val window =
        FloatArray(FFT_SIZE) { i ->
            (0.54 - 0.46 * cos(2.0 * PI * i / (FFT_SIZE - 1))).toFloat()
        }
    private val sampleBuffer = FloatArray(FFT_SIZE)
    private var bufferedSamples = 0
    private val fftReal = FloatArray(FFT_SIZE)
    private val fftImag = FloatArray(FFT_SIZE)
    private val smoothed = FloatArray(BAND_COUNT)
    private var sampleRateHz = 44_100
    private var channelCount = 2
    private var encoding = C.ENCODING_PCM_16BIT
    private var lastEmitMs = 0L

    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
        if (!enabled) reset()
    }

    fun reset() {
        bufferedSamples = 0
        smoothed.fill(0f)
        sampleBuffer.fill(0f)
        _levels.value = FloatArray(BAND_COUNT)
    }

    override fun flush(
        sampleRateHz: Int,
        channelCount: Int,
        encoding: Int,
    ) {
        this.sampleRateHz = sampleRateHz
        this.channelCount = channelCount.coerceAtLeast(1)
        this.encoding = encoding
        bufferedSamples = 0
    }

    override fun handleBuffer(buffer: ByteBuffer) {
        if (!isEnabled) return
        val isFloat = encoding == C.ENCODING_PCM_FLOAT
        if (encoding != C.ENCODING_PCM_16BIT && !isFloat) return
        // Never disturb the original buffer: the tee passes it downstream to the speakers.
        val input = buffer.duplicate().order(ByteOrder.LITTLE_ENDIAN)
        val bytesPerFrame = channelCount * if (isFloat) 4 else 2
        if (bytesPerFrame <= 0) return
        val frames = input.remaining() / bytesPerFrame
        repeat(frames) {
            var mono = 0f
            repeat(channelCount) {
                mono += if (isFloat) input.float else input.short / 32_768f
            }
            sampleBuffer[bufferedSamples++] = mono / channelCount
            if (bufferedSamples == FFT_SIZE) {
                analyze()
                bufferedSamples = 0
            }
        }
    }

    private fun analyze() {
        for (i in 0 until FFT_SIZE) {
            fftReal[i] = sampleBuffer[i] * window[i]
            fftImag[i] = 0f
        }
        fft(fftReal, fftImag)

        val binHz = sampleRateHz.toFloat() / FFT_SIZE
        val maxHz = (sampleRateHz / 2f).coerceAtMost(MAX_HZ)
        val logMin = ln(MIN_HZ)
        val logRange = ln(maxHz) - logMin
        // A full-scale sine peaks at FFT_SIZE / 2, so this normalizes magnitudes to 0..1.
        val scale = 2f / FFT_SIZE
        var changed = false
        for (band in 0 until BAND_COUNT) {
            val lowHz = exp(logMin + logRange * band / BAND_COUNT)
            val highHz = exp(logMin + logRange * (band + 1) / BAND_COUNT)
            val lowBin = (lowHz / binHz).toInt().coerceIn(1, FFT_SIZE / 2 - 1)
            val highBin = (highHz / binHz).toInt().coerceIn(lowBin + 1, FFT_SIZE / 2)
            var peak = 0f
            for (bin in lowBin until highBin) {
                val re = fftReal[bin]
                val im = fftImag[bin]
                val magnitude = sqrt(re * re + im * im) * scale
                if (magnitude > peak) peak = magnitude
            }
            val target = sqrt(peak.coerceIn(0f, 1f))
            val previous = smoothed[band]
            val next =
                if (target > previous) {
                    previous + (target - previous) * ATTACK
                } else {
                    previous * RELEASE
                }
            if (abs(next - previous) > 0.001f) changed = true
            smoothed[band] = next
        }

        val now = android.os.SystemClock.elapsedRealtime()
        if (changed && now - lastEmitMs >= EMIT_INTERVAL_MS) {
            lastEmitMs = now
            _levels.value = smoothed.copyOf()
        }
    }

    /** In-place iterative radix-2 FFT. Size must be a power of two. */
    private fun fft(
        real: FloatArray,
        imag: FloatArray,
    ) {
        val n = real.size
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) {
                j = j and bit.inv()
                bit = bit shr 1
            }
            j = j or bit
            if (i < j) {
                val tempReal = real[i]
                real[i] = real[j]
                real[j] = tempReal
                val tempImag = imag[i]
                imag[i] = imag[j]
                imag[j] = tempImag
            }
        }
        var length = 2
        while (length <= n) {
            val angle = -2.0 * PI / length
            val wReal = cos(angle).toFloat()
            val wImag = sin(angle).toFloat()
            var i = 0
            while (i < n) {
                var wr = 1f
                var wi = 0f
                for (k in 0 until length / 2) {
                    val ur = real[i + k]
                    val ui = imag[i + k]
                    val vr = real[i + k + length / 2] * wr - imag[i + k + length / 2] * wi
                    val vi = real[i + k + length / 2] * wi + imag[i + k + length / 2] * wr
                    real[i + k] = ur + vr
                    imag[i + k] = ui + vi
                    real[i + k + length / 2] = ur - vr
                    imag[i + k + length / 2] = ui - vi
                    val nextWr = wr * wReal - wi * wImag
                    wi = wr * wImag + wi * wReal
                    wr = nextWr
                }
                i += length
            }
            length = length shl 1
        }
    }
}
