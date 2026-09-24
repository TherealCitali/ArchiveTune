/*
 * LunarTune (2026)
 * © cognitiveshadows03 — github.com/cognitiveshadows03
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.citali.lunartune.ui.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.floatPreferencesKey
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect

/** Default dim: scrim over the live blur (0-90% adjustable in settings). */
const val FrostedDimDefault = 0.6f

/** Default blur radius in dp (0-30 adjustable in settings). */
const val FrostedBlurDefault = 10f

/** Default primary-color rim glow around the frosted pill (0-100% adjustable). */
const val FrostedGlowDefault = 0.5f

val FrostedDimKey = floatPreferencesKey("frostedDim")
val FrostedBlurKey = floatPreferencesKey("frostedBlur")
val FrostedGlowKey = floatPreferencesKey("frostedGlow")

/** Opaque-ish scrim used only if blurring is unavailable on the device. */
const val FrostedFallbackAlpha = 0.92f

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
    blurRadiusDp: Float = FrostedBlurDefault,
): HazeBlurStyle =
    HazeBlurStyle {
        blurEnabled(true)
        blurRadius(blurRadiusDp.coerceIn(0f, 30f).dp)
        backgroundColor(scrim)
        colorEffects(emptyList())
        fallbackColorEffect(HazeColorEffect.tint(fallbackScrim))
        noiseFactor(0f)
    }
