/*
 * LunarTune (2026)
 * © cognitiveshadows03 — github.com/cognitiveshadows03
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.citali.lunartune.ui.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import dev.citali.lunartune.models.MediaMetadata
import dev.citali.lunartune.ui.utils.highRes

@Composable
fun VinylPlayerStage(
    mediaMetadata: MediaMetadata,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val slide = remember { Animatable(0f) }
    val armProgress = remember { Animatable(0f) }

    LaunchedEffect(mediaMetadata.id) {
        slide.snapTo(1f)
        armProgress.snapTo(0f)
        slide.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 720, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        )
        delay(80)
        armProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 520, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        )
    }

    LaunchedEffect(isPlaying, mediaMetadata.id) {
        if (slide.value > 0.05f) return@LaunchedEffect
        armProgress.animateTo(
            targetValue = if (isPlaying) 1f else 0.22f,
            animationSpec = tween(durationMillis = 280),
        )
    }

    val spinTransition = rememberInfiniteTransition(label = "vinylSpin")
    val spin by spinTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 3600, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "vinylSpinAngle",
    )
    val discRotation = if (isPlaying) spin else 0f

    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxSize()
                .clipToBounds()
                .background(Color.Black),
    ) {
        val discSize = maxOf(maxHeight * 1.08f, maxWidth * 0.92f)
        val discPx = with(density) { discSize.toPx() }
        val slidePx = discPx * 0.92f * slide.value

        Box(
            modifier =
                Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = discSize * 0.46f)
                    .graphicsLayer {
                        translationX = slidePx
                    },
        ) {
            VinylDisc(
                artworkUrl = mediaMetadata.thumbnailUrl?.highRes() ?: mediaMetadata.thumbnailUrl,
                rotationDegrees = discRotation,
                modifier = Modifier.size(discSize),
            )
        }

        VinylTonearm(
            progress = armProgress.value,
            modifier =
                Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = (-12).dp, y = (-maxHeight * 0.08f)),
        )
    }
}

@Composable
private fun VinylDisc(
    artworkUrl: String?,
    rotationDegrees: Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier.graphicsLayer {
                rotationZ = rotationDegrees
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(color = Color(0xFF111111), radius = radius, center = center)
            drawCircle(color = Color(0xFF1C1C1C), radius = radius * 0.985f, center = center)
            val grooveCount = 18
            for (i in 1..grooveCount) {
                val t = i / (grooveCount + 1f)
                val r = radius * (0.28f + 0.66f * t)
                drawCircle(
                    color = Color.White.copy(alpha = if (i % 2 == 0) 0.07f else 0.04f),
                    radius = r,
                    center = center,
                    style = Stroke(width = 1.6f),
                )
            }
            drawCircle(
                color = Color(0xFF2A2A2A),
                radius = radius * 0.26f,
                center = center,
            )
            drawCircle(
                color = Color(0xFF0A0A0A),
                radius = radius * 0.035f,
                center = center,
            )
        }
        Box(
            modifier =
                Modifier
                    .fillMaxSize(0.42f)
                    .clip(CircleShape)
                    .background(Color(0xFF222222)),
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
    val rotation = -48f + 62f * progress
    Canvas(
        modifier =
            modifier
                .size(width = 168.dp, height = 280.dp)
                .graphicsLayer {
                    transformOrigin = TransformOrigin(0.18f, 0.12f)
                    rotationZ = rotation
                },
    ) {
        val pivot = Offset(size.width * 0.18f, size.height * 0.12f)
        val tip = Offset(size.width * 0.88f, size.height * 0.78f)
        drawCircle(color = Color(0xFFB8B8B8), radius = 16.dp.toPx(), center = pivot)
        drawCircle(color = Color(0xFF3A3A3A), radius = 7.dp.toPx(), center = pivot)
        drawLine(
            color = Color(0xFFD0D0D0),
            start = pivot,
            end = tip,
            strokeWidth = 7.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawCircle(color = Color(0xFFE8E8E8), radius = 10.dp.toPx(), center = tip)
        drawCircle(color = Color(0xFFC45C2A), radius = 3.4.dp.toPx(), center = tip)
    }
}
