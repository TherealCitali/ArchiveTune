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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Pre-blurs the playing track's cover on a background thread so the lyrics
 * page can open on an already-blurred bitmap instead of a sharp cover.
 */
object LyricsArtBlurCache {
    // Both consumers draw this across the whole lyrics page — the Apple Music background fills
    // the screen and the moving-blur backdrop magnifies it further — so it is decoded and blurred
    // at a size that survives being scaled up, rather than at thumbnail size.
    private const val DecodeSizePx = 384
    private const val BlurRadius = 32f
    private const val MaxEntries = 8

    private val mutex = Mutex()
    private val bitmaps =
        object : LinkedHashMap<String, Bitmap>(MaxEntries, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap>): Boolean =
                size > MaxEntries
        }
    private val inFlight = mutableSetOf<String>()
    private val revision = MutableStateFlow(0)

    val updates: StateFlow<Int> = revision.asStateFlow()

    fun peek(url: String?): Bitmap? {
        if (url.isNullOrBlank()) return null
        synchronized(bitmaps) {
            return bitmaps[url]
        }
    }

    suspend fun prefetch(
        context: Context,
        url: String?,
    ) {
        if (url.isNullOrBlank()) return
        if (peek(url) != null) return

        val shouldLoad =
            mutex.withLock {
                if (url in inFlight || peek(url) != null) {
                    false
                } else {
                    inFlight.add(url)
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
                    ImageBlurUtils.blur(bitmap, BlurRadius)
                } ?: return

            synchronized(bitmaps) {
                bitmaps[url] = blurred
            }
            revision.value = revision.value + 1
        } finally {
            mutex.withLock { inFlight.remove(url) }
        }
    }
}
