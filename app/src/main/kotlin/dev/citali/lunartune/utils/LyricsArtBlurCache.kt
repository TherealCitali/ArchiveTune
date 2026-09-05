/*
 * LunarTune (2026)
 * © cognitiveshadows03 — github.com/cognitiveshadows03
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.citali.lunartune.utils

import android.content.Context
import android.graphics.Bitmap
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.size.Size
import coil3.toBitmap
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Pre-blurs the playing track's cover on a background thread so a surface that needs blurred
 * artwork can show an already-blurred bitmap instead of a sharp cover.
 *
 * Started out serving only the lyrics page — hence the name — but it is now also how the player's
 * blur backgrounds get their blur on Android 11 and below, where `Modifier.blur` does nothing at
 * all. One blur per track, shared by every surface that needs one.
 */
object LyricsArtBlurCache {
    // Both consumers draw this across the whole lyrics page — the Apple Music background fills
    // the screen and the moving-blur backdrop magnifies it further — so it is decoded and blurred
    // at a size that survives being scaled up, rather than at thumbnail size.
    private const val DecodeSizePx = 384
    private const val BlurRadius = 32f
    private const val MaxEntries = 8

    /**
     * Blur radii are rounded to this before they are used as a cache key. A blur slider would
     * otherwise fill the cache with one entry per value it is dragged through.
     */
    private const val RadiusStep = 8f

    private val mutex = Mutex()
    private val bitmaps =
        object : LinkedHashMap<String, Bitmap>(MaxEntries, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap>): Boolean =
                size > MaxEntries
        }
    private val inFlight = mutableSetOf<String>()
    private val revision = MutableStateFlow(0)

    val updates: StateFlow<Int> = revision.asStateFlow()

    fun peek(
        url: String?,
        radius: Float = BlurRadius,
    ): Bitmap? {
        if (url.isNullOrBlank()) return null
        val key = key(url, radius)
        synchronized(bitmaps) {
            return bitmaps[key]
        }
    }

    suspend fun prefetch(
        context: Context,
        url: String?,
        radius: Float = BlurRadius,
    ) {
        if (url.isNullOrBlank()) return
        val key = key(url, radius)
        if (peek(url, radius) != null) return

        val shouldLoad =
            mutex.withLock {
                if (key in inFlight || peek(url, radius) != null) {
                    false
                } else {
                    inFlight.add(key)
                    true
                }
            }
        if (!shouldLoad) return

        try {
            val blurred =
                withContext(Dispatchers.IO) {
                    val request =
                        ImageRequest
                            .Builder(context.applicationContext)
                            .data(url)
                            .size(Size(DecodeSizePx, DecodeSizePx))
                            .allowHardware(false)
                            .build()
                    val image = context.applicationContext.imageLoader.execute(request).image ?: return@withContext null
                    val bitmap = image.toBitmap().copy(Bitmap.Config.ARGB_8888, true)
                    ImageBlurUtils.blur(bitmap, quantize(radius))
                } ?: return

            synchronized(bitmaps) {
                bitmaps[key] = blurred
            }
            revision.value = revision.value + 1
        } finally {
            mutex.withLock { inFlight.remove(key) }
        }
    }

    private fun key(
        url: String,
        radius: Float,
    ): String = "$url#${quantize(radius).toInt()}"

    private fun quantize(radius: Float): Float =
        (radius / RadiusStep).roundToInt() * RadiusStep
}
