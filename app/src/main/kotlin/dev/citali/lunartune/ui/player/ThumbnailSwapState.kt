/*
 * LunarTune (2026)
 * © cognitiveshadows03 — github.com/cognitiveshadows03
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.citali.lunartune.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import coil3.imageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import dev.citali.lunartune.ui.utils.YTThumbQuality
import dev.citali.lunartune.ui.utils.buildYTThumbnailUrl
import dev.citali.lunartune.ui.utils.resize
import timber.log.Timber

private const val HighResMinEdgePx = 480

@Immutable
data class ThumbnailSwapState(
    val displayUrl: String?,
    val isYTReady: Boolean,
    val ytUrl: String?,
)

@Composable
fun rememberThumbnailSwapState(
    videoId: String?,
    ytmUrl: String?,
    lowDataMode: Boolean,
    isMusicVideo: Boolean = false,
): ThumbnailSwapState {
    val context = LocalContext.current
    val shouldAttemptHighRes = videoId != null && !lowDataMode

    var displayUrl by remember { mutableStateOf(ytmUrl) }
    var isYTReady by remember { mutableStateOf(false) }
    var ytUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(videoId, ytmUrl, shouldAttemptHighRes, isMusicVideo) {
        displayUrl = ytmUrl
        isYTReady = false
        ytUrl = null

        if (!shouldAttemptHighRes || videoId == null) return@LaunchedEffect

        val imageLoader = context.imageLoader
        val candidates = buildHighResArtworkCandidates(videoId, ytmUrl)

        for (url in candidates) {
            if (url.isBlank() || url == ytmUrl) continue
            try {
                val request =
                    ImageRequest
                        .Builder(context)
                        .data(url)
                        .memoryCacheKey(url)
                        .diskCacheKey(url)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .networkCachePolicy(CachePolicy.ENABLED)
                        .allowHardware(false)
                        .size(1080)
                        .build()
                val result =
                    withContext(Dispatchers.IO) {
                        imageLoader.execute(request)
                    }
                if (result is SuccessResult) {
                    val bitmap = result.image.toBitmap()
                    if (minOf(bitmap.width, bitmap.height) < HighResMinEdgePx) continue
                    ytUrl = url
                    displayUrl = url
                    isYTReady = true
                    return@LaunchedEffect
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.tag("ThumbnailSwap").d(e, "High-res artwork failed: %s", url)
                continue
            }
        }
        displayUrl = ytmUrl
    }

    return ThumbnailSwapState(displayUrl, isYTReady, ytUrl)
}

internal fun buildHighResArtworkCandidates(
    videoId: String,
    ytmUrl: String?,
): List<String> {
    val urls = linkedSetOf<String>()
    urls += buildYTThumbnailUrl(videoId, YTThumbQuality.MAXRES)
    urls += "https://i.ytimg.com/vi_webp/$videoId/maxresdefault.webp"
    urls += buildYTThumbnailUrl(videoId, YTThumbQuality.HQ720)
    urls += "https://i.ytimg.com/vi_webp/$videoId/hq720.webp"
    val upscaledYtm =
        ytmUrl
            ?.takeIf { it.isNotBlank() }
            ?.resize(width = 2048, height = 2048, maxresAllowed = true)
    if (!upscaledYtm.isNullOrBlank()) urls += upscaledYtm
    urls += buildYTThumbnailUrl(videoId, YTThumbQuality.HQ)
    return urls.toList()
}
