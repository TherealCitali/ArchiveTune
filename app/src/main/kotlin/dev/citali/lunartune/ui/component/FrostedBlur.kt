/*
 * LunarTune (2026)
 * © cognitiveshadows03 — github.com/cognitiveshadows03
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.citali.lunartune.ui.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect

/** Scrim over the live blur, matching AniDash's 50% surface veil. */
const val FrostedScrimAlpha = 0.5f

/** Opaque-ish scrim used only if blurring is unavailable on the device. */
const val FrostedFallbackAlpha = 0.92f

/** Primary-color rim around the frosted pill, like AniDash's outlined bar. */
const val FrostedBorderAlpha = 0.5f

/**
 * AniDash-style frosted blur for the navigation bar: a small live-blur radius
 * over a 50% surface scrim. The radius stays tiny on purpose — a small kernel
 * over a small pill is what keeps this at full fps on weak GPUs.
 *
 * blurEnabled(true) forces Haze's RenderScript path on Android 11 and below
 * (Haze's default there is a static scrim with no blur).
 */
fun frostedNavBarStyle(
    scrim: Color,
    fallbackScrim: Color,
): HazeBlurStyle =
    HazeBlurStyle {
        blurEnabled(true)
        blurRadius(10.dp)
        backgroundColor(scrim)
        colorEffects(emptyList())
        fallbackColorEffect(HazeColorEffect.tint(fallbackScrim))
        noiseFactor(0f)
    }
