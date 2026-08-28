/*
 * LunarTune (2026)
 * © cognitiveshadows03 — github.com/cognitiveshadows03
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.citali.lunartune.ui.player

import android.graphics.Bitmap
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.size.Size
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import dev.citali.lunartune.constants.EnableHapticFeedbackKey
import dev.citali.lunartune.models.MediaMetadata
import dev.citali.lunartune.ui.utils.highRes
import dev.citali.lunartune.utils.rememberPreference
import kotlin.math.exp

private const val VinylRpm = 33.333f
private val VinylSpring =
    spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )

@Composable
fun VinylPlayerStage(
    mediaMetadata: MediaMetadata,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val density = LocalDensity.current
    val (enableHapticFeedback) = rememberPreference(EnableHapticFeedbackKey, true)
    val artworkUrl = mediaMetadata.thumbnailUrl?.highRes() ?: mediaMetadata.thumbnailUrl

    val reveal = remember { Animatable(if (isPlaying) 1f else 0f) }
    val arm = remember { Animatable(if (isPlaying) 1f else 0.12f) }
    val rotation = remember { Animatable(0f) }
    var labelColor by remember(artworkUrl) { mutableStateOf(Color(0xFF2A1810)) }
    var grooveTint by remember(artworkUrl) { mutableStateOf(Color(0xFF3A2418)) }

    LaunchedEffect(artworkUrl) {
        val url = artworkUrl ?: return@LaunchedEffect
        val extracted =
            withContext(Dispatchers.IO) {
                runCatching {
                    val request =
                        ImageRequest
                            .Builder(context)
                            .data(url)
                            .size(Size(96, 96))
                            .allowHardware(false)
                            .build()
                    val image = context.imageLoader.execute(request).image ?: return@runCatching null
                    val bitmap = image.toBitmap().copy(Bitmap.Config.ARGB_8888, false)
                    val palette = Palette.from(bitmap).clearFilters().generate()
                    val rgb = palette.dominantSwatch?.rgb ?: palette.getDominantColor(0xFF2A1810.toInt())
                    Color(rgb)
                }.getOrNull()
            } ?: return@LaunchedEffect
        labelColor = extracted.copy(alpha = 1f)
        grooveTint = extracted.copy(alpha = 1f)
    }

    LaunchedEffect(mediaMetadata.id) {
        arm.animateTo(0f, VinylSpring)
        reveal.animateTo(1.35f, VinylSpring)
        reveal.snapTo(1.35f)
        reveal.animateTo(if (isPlaying) 1f else 0f, VinylSpring)
        arm.animateTo(if (isPlaying) 1f else 0.14f, VinylSpring)
        if (isPlaying && enableHapticFeedback) {
            val haptic =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    HapticFeedbackConstants.CONFIRM
                } else {
                    HapticFeedbackConstants.CONTEXT_CLICK
                }
            view.performHapticFeedback(haptic, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)
        }
    }

    LaunchedEffect(isPlaying) {
        launch {
            reveal.animateTo(if (isPlaying) 1f else 0f, VinylSpring)
        }
        launch {
            delay(if (isPlaying) 90 else 0)
            arm.animateTo(if (isPlaying) 1f else 0.16f, VinylSpring)
            if (isPlaying && enableHapticFeedback) {
                view.performHapticFeedback(
                    HapticFeedbackConstants.CONTEXT_CLICK,
                    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING,
                )
            }
        }
    }

    LaunchedEffect(isPlaying) {
        val degreesPerSecond = 360f * (VinylRpm / 60f)
        if (isPlaying) {
            var last = 0L
            while (isActive) {
                withFrameNanos { frame ->
                    if (last != 0L) {
                        val dt = ((frame - last) / 1_000_000_000f).coerceIn(0f, 0.05f)
                        val next = (rotation.value + degreesPerSecond * dt) % 360f
                        rotation.snapTo(next)
                    }
                    last = frame
                }
            }
        } else {
            var velocity = degreesPerSecond
            var last = 0L
            while (isActive && velocity > 1.2f) {
                withFrameNanos { frame ->
                    if (last != 0L) {
                        val dt = ((frame - last) / 1_000_000_000f).coerceIn(0f, 0.05f)
                        velocity *= exp(-2.6f * dt)
                        val next = (rotation.value + velocity * dt) % 360f
                        rotation.snapTo(next)
                    }
                    last = frame
                }
            }
        }
    }

    val stageColor =
        Color(
            android.graphics.Color.HSVToColor(
                FloatArray(3).also { hsv ->
                    android.graphics.Color.colorToHSV(labelColor.toArgb(), hsv)
                    hsv[1] = (hsv[1] * 0.55f).coerceIn(0.12f, 0.5f)
                    hsv[2] = 0.10f
                },
            ),
        )

    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxSize()
                .clipToBounds()
                .background(stageColor),
    ) {
        val discSize = maxOf(maxHeight * 0.98f, maxWidth * 0.86f)
        val peek = with(density) { (discSize * 0.46f).toPx() }
        val playIn = with(density) { (discSize * 0.14f).toPx() }
        val extraOut = with(density) { (discSize * 0.55f).toPx() }
        val revealValue = reveal.value
        val translation =
            when {
                revealValue <= 0f -> peek
                revealValue <= 1f -> peek + (playIn - peek) * revealValue
                else -> playIn + extraOut * (revealValue - 1f)
            }

        Box(
            modifier =
                Modifier
                    .align(Alignment.CenterEnd)
                    .graphicsLayer { translationX = translation }
                    .drawBehind {
                        val r = size.minDimension / 2f
                        drawCircle(
                            color = Color.Black.copy(alpha = 0.45f),
                            radius = r * 0.96f,
                            center = Offset(size.width / 2f + 10.dp.toPx(), size.height / 2f + 18.dp.toPx()),
                        )
                    },
        ) {
            VinylDisc(
                artworkUrl = artworkUrl,
                rotationDegrees = rotation.value,
                grooveTint = grooveTint,
                modifier = Modifier.size(discSize),
            )
        }

        VinylTonearm(
            progress = arm.value.coerceIn(0f, 1f),
            modifier =
                Modifier
                    .align(Alignment.TopStart)
                    .offset(x = 8.dp, y = maxHeight * 0.06f),
        )
    }
}

@Composable
private fun VinylDisc(
    artworkUrl: String?,
    rotationDegrees: Float,
    grooveTint: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier.graphicsLayer {
                rotationZ = rotationDegrees
                shadowElevation = 18f
                shape = CircleShape
                clip = false
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(color = Color(0xFF0C0C0C), radius = radius, center = center)
            drawCircle(color = Color(0xFF161616), radius = radius * 0.988f, center = center)
            val grooveCount = 22
            for (i in 1..grooveCount) {
                val t = i / (grooveCount + 1f)
                val r = radius * (0.30f + 0.64f * t)
                drawCircle(
                    color = grooveTint.copy(alpha = if (i % 2 == 0) 0.16f else 0.08f),
                    radius = r,
                    center = center,
                    style = Stroke(width = 1.8f),
                )
            }
            drawCircle(
                brush =
                    Brush.radialGradient(
                        colors =
                            listOf(
                                Color.White.copy(alpha = 0.18f),
                                Color.Transparent,
                            ),
                        center = Offset(center.x - radius * 0.28f, center.y - radius * 0.34f),
                        radius = radius * 0.72f,
                    ),
                radius = radius,
                center = center,
            )
            drawCircle(color = Color(0xFF1A1A1A), radius = radius * 0.30f, center = center)
            drawCircle(color = Color(0xFF080808), radius = radius * 0.032f, center = center)
        }
        Box(
            modifier =
                Modifier
                    .fillMaxSize(0.48f)
                    .clip(CircleShape)
                    .background(grooveTint.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center,
        ) {
            if (artworkUrl != null) {
                AsyncImage(
                    model = artworkUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun VinylTonearm(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val lifted = 1f - progress
    val rotation = -38f + 54f * progress
    Canvas(
        modifier =
            modifier
                .size(width = 220.dp, height = 320.dp)
                .graphicsLayer {
                    transformOrigin = TransformOrigin(0.14f, 0.10f)
                    rotationZ = rotation
                    translationY = -18.dp.toPx() * lifted
                },
    ) {
        val pivot = Offset(size.width * 0.14f, size.height * 0.10f)
        val mid = Offset(size.width * 0.62f, size.height * 0.48f)
        val tip = Offset(size.width * 0.86f, size.height * 0.72f)
        drawCircle(color = Color(0xFFCFCFCF), radius = 18.dp.toPx(), center = pivot)
        drawCircle(color = Color(0xFF3A3A3A), radius = 8.dp.toPx(), center = pivot)
        drawLine(Color(0xFFE4E4E4), pivot, mid, 8.dp.toPx(), StrokeCap.Round)
        drawLine(Color(0xFFD8D8D8), mid, tip, 6.dp.toPx(), StrokeCap.Round)
        drawCircle(color = Color(0xFFF0F0F0), radius = 11.dp.toPx(), center = tip)
        drawCircle(color = Color(0xFFC45C2A), radius = 3.6.dp.toPx(), center = tip)
    }
}
