/*
 * LunarTune (2026)
 * © cognitiveshadows03 — github.com/cognitiveshadows03
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.citali.lunartune.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.spring

/**
 * Central motion specs for LunarTune.
 *
 * Springs replace the default stiff tweens on interactive motion (presses, slides, chevrons,
 * snap-backs) so movement settles organically instead of stopping dead. Curves intentionally use
 * literal stiffness/damping values rather than the [Spring] presets so the feel stays identical
 * across Compose versions.
 *
 * Deliberately NOT sprung: color morphs (channels overshoot into flashes), alpha fades, lyric
 * motion, ambient/AOD motion, and loading choreography — tweens remain correct there.
 */
object LunarMotion {
    /** MD3 emphasized decelerate — elements entering the screen. */
    val EmphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    /** MD3 emphasized accelerate — elements leaving the screen. */
    val EmphasizedAccelerate: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    /**
     * Gentle bounce for entrances, icon rotations, sheet panels and swipe snap-backs.
     * Settles in ~280ms with one soft overshoot.
     */
    fun <T> bouncy() = spring<T>(dampingRatio = 0.72f, stiffness = 800f)

    /**
     * Organic settle with zero overshoot, for progress/shape-driven motion (search morph,
     * crossfades with scale) where overshooting past the target would distort layout.
     */
    fun <T> smooth() = spring<T>(dampingRatio = 1f, stiffness = 1100f)

    /**
     * Fast retract for exits (FAB scrolling away, banners dismissing). Barely-there give,
     * gone in ~150ms.
     */
    fun <T> snappy() = spring<T>(dampingRatio = 0.95f, stiffness = 2600f)

    /**
     * Full-screen glide for nav transitions (tab blooms, drill slides). A whisper of
     * overshoot over ~450ms so page changes read clearly without seasickness.
     */
    fun <T> glide() = spring<T>(dampingRatio = 0.86f, stiffness = 450f)

    /** Tactile press bounce for [dev.citali.lunartune.ui.component.IconButton]. */
    fun press() = spring<Float>(dampingRatio = 0.6f, stiffness = 1500f)

    /** Scale target while pressed. */
    const val PressedScale = 0.85f
}
