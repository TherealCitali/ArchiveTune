/*
 * LunarTune (2026)
 * © cognitiveshadows03 — github.com/cognitiveshadows03
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.citali.lunartune.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.citali.lunartune.R
import dev.citali.lunartune.constants.ThumbnailCornerRadius

@Composable
fun PlayingIndicator(
    color: Color,
    modifier: Modifier = Modifier,
    bars: Int = 3,
    barWidth: Dp = 4.dp,
    cornerRadius: Dp = ThumbnailCornerRadius,
) {
    // One infinite transition drives every bar. The previous version ran one coroutine per bar
    // that picked a new Random target every ~50 ms and started a fresh Animatable spring for it —
    // three always-on coroutines and a per-step allocation for each indicator on screen (there
    // is one per row in the queue / library lists). The staggered keyframes look the same.
    val barCount = bars.coerceAtLeast(1)
    val transition = rememberInfiniteTransition(label = "playingIndicator")
    val barValues =
        List(barCount) { index ->
            val phaseStep = PlayingIndicatorCycleMs / (barCount + 1)
            val delayMs = (barCount - index - 1) * phaseStep
            transition.animateFloat(
                initialValue = 0.1f,
                targetValue = 1f,
                animationSpec =
                    infiniteRepeatable(
                        animation =
                            keyframes {
                                durationMillis = PlayingIndicatorCycleMs
                                0.1f at 0
                                1f at 360
                                0.3f at 660
                                0.1f at PlayingIndicatorCycleMs
                            },
                        repeatMode = RepeatMode.Restart,
                        initialStartOffset = StartOffset(delayMs),
                    ),
                label = "bar$index",
            )
        }

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = modifier,
    ) {
        barValues.forEach { barValue ->
            Canvas(
                modifier =
                    Modifier
                        .fillMaxHeight()
                        .width(barWidth),
            ) {
                // Read in the draw phase only: the animation ticks redraw the bar without
                // recomposing the indicator.
                val value = barValue.value
                drawRoundRect(
                    color = color,
                    topLeft = Offset(x = 0f, y = size.height * (1 - value)),
                    size = size.copy(height = value * size.height),
                    cornerRadius = CornerRadius(cornerRadius.toPx()),
                )
            }
        }
    }
}

private const val PlayingIndicatorCycleMs = 1100

@Composable
fun PlayingIndicatorBox(
    modifier: Modifier = Modifier,
    isActive: Boolean,
    playWhenReady: Boolean,
    color: Color = LocalContentColor.current,
) {
    AnimatedVisibility(
        visible = isActive,
        enter = fadeIn(tween(500)),
        exit = fadeOut(tween(500)),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier,
        ) {
            if (playWhenReady) {
                PlayingIndicator(
                    color = color,
                    modifier = Modifier.height(24.dp),
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.play),
                    contentDescription = null,
                    tint = color,
                )
            }
        }
    }
}
