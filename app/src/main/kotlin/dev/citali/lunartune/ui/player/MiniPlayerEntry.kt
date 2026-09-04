/*
 * LunarTune (2026)
 * © cognitiveshadows03 — github.com/cognitiveshadows03
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.citali.lunartune.ui.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.citali.lunartune.constants.DisableAnimationsKey
import dev.citali.lunartune.constants.MiniPlayerHeight
import dev.citali.lunartune.utils.rememberPreference
import kotlin.math.min

/** How long the mini player takes to unfold from the pill. */
private const val MINI_PLAYER_ENTRY_MS = 520

/** Size of the pill at the very start of the unfold. */
private val PillHeight = 48.dp
private const val PillWidthFraction = 0.42f

/**
 * How far below its resting place the pill starts.
 *
 * Capped at half the difference between the bar and the pill, which puts the pill flush with the
 * bottom of the mini player's own bounds — sitting on the seam directly above the navigation bar.
 * It cannot go lower without disappearing: the floating navigation bar is composed *after* the
 * player sheet in `MainActivity`, so it paints over anything the mini player draws below its box.
 */
private val PillRise = (MiniPlayerHeight - PillHeight) / 2

/** Fraction of the unfold over which the pill fades up from nothing. */
private const val PillAlphaFraction = 0.18f

/**
 * Progress of the mini player's entrance, `0f` = a pill sitting on the navigation bar, `1f` = the
 * finished mini player. Sits at `1f` except while a fresh entrance is playing.
 *
 * The mini player is only composed while the sheet is not dismissed, so a **fresh** composition is
 * exactly the moment the sheet came back — which is what starting playback does. The one case that
 * is not a playback start is the first composition of the process, where a track may simply have
 * been restored from the last session; that one is adopted silently.
 *
 * Pausing and resuming never animates: the track is still there, so the composable is never torn
 * down and [hasPlayback] never goes false.
 */
@Composable
internal fun rememberMiniPlayerEntryProgress(hasPlayback: Boolean): State<Float> {
    val disableAnimations by rememberPreference(DisableAnimationsKey, false)
    val progress = remember { Animatable(1f) }

    // 0 = not observed yet, 1 = a track was present, 2 = nothing was playing.
    var lastObservation by rememberSaveable { mutableIntStateOf(0) }
    var primed by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(hasPlayback, disableAnimations) {
        val previous = lastObservation
        lastObservation = if (hasPlayback) 1 else 2

        if (disableAnimations) return@LaunchedEffect

        if (!primed) {
            primed = true
            val firstCompositionOfProcess = !wasMiniPlayerComposed
            wasMiniPlayerComposed = true
            // A fresh composition with a track already loaded is playback starting, unless it is
            // the first one of the process — that is the app opening onto a restored session.
            if (!firstCompositionOfProcess && hasPlayback) unfold(progress)
            return@LaunchedEffect
        }

        // Steady state: only a transition *into* playback counts — a track change while playing
        // leaves [hasPlayback] true, so it does not replay the entrance.
        if (previous == 2 && hasPlayback) unfold(progress)
    }

    return progress.asState()
}

private suspend fun unfold(progress: Animatable<Float, AnimationVector1D>) {
    progress.snapTo(0f)
    progress.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = MINI_PLAYER_ENTRY_MS, easing = FastOutSlowInEasing),
    )
}

/**
 * True once the mini player has been composed at least once since the process started. Process
 * scope, not composition scope, on purpose — it is what distinguishes "the app opened with music
 * already playing" from "playback just began".
 */
private var wasMiniPlayerComposed = false

/**
 * Clips the mini player to a pill that unfolds into the finished bar.
 *
 * Everything here is a draw-phase read of [progress]: the bar is never re-measured and never
 * re-composed while it animates. The window is a clip rather than a layout change, so the artwork,
 * title and controls keep their real size throughout and are simply *revealed* as the window opens
 * — which is what stops the unfold from looking like the contents are being stretched.
 */
internal fun Modifier.unfoldFromPill(
    progress: () -> Float,
    restingShape: Shape,
): Modifier =
    this.graphicsLayer {
        val p = progress().coerceIn(0f, 1f)
        clip = true
        shape = UnfoldingPillShape(progress = p, restingShape = restingShape)
        translationY = PillRise.toPx() * (1f - p)
        alpha = (p / PillAlphaFraction).coerceIn(0f, 1f)
    }

private class UnfoldingPillShape(
    private val progress: Float,
    private val restingShape: Shape,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        // Finished: hand the exact resting shape back so there is no second, subtly different
        // clip once the animation is over.
        if (progress >= 1f) return restingShape.createOutline(size, layoutDirection, density)

        val width = lerp(size.width * PillWidthFraction, size.width, progress)
        val height = lerp(with(density) { PillHeight.toPx() }, size.height, progress)
        val left = (size.width - width) / 2f
        val top = (size.height - height) / 2f
        val pillRadius = min(width, height) / 2f

        // Corners walk from a full pill to whatever the bar rests at — 28/28/12/12 when it is
        // paired with the navigation bar, the theme's extra-large corner otherwise.
        val resting = restingShape as? CornerBasedShape
        val topStart = resting?.topStart?.toPx(size, density) ?: pillRadius
        val topEnd = resting?.topEnd?.toPx(size, density) ?: pillRadius
        val bottomEnd = resting?.bottomEnd?.toPx(size, density) ?: pillRadius
        val bottomStart = resting?.bottomStart?.toPx(size, density) ?: pillRadius

        return Outline.Rounded(
            RoundRect(
                rect = Rect(left, top, left + width, top + height),
                topLeft = CornerRadius(lerp(pillRadius, topStart, progress)),
                topRight = CornerRadius(lerp(pillRadius, topEnd, progress)),
                bottomRight = CornerRadius(lerp(pillRadius, bottomEnd, progress)),
                bottomLeft = CornerRadius(lerp(pillRadius, bottomStart, progress)),
            ),
        )
    }
}

private fun lerp(
    start: Float,
    stop: Float,
    fraction: Float,
): Float = start + (stop - start) * fraction
