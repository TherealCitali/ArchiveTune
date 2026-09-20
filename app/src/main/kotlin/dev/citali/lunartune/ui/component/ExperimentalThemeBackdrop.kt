/*
 * LunarTune (2026)
 * © cognitiveshadows03 — github.com/cognitiveshadows03
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.citali.lunartune.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.material3.MaterialTheme

/**
 * Soft theme-derived colour atmosphere for the opt-in experimental surfaces.
 * It stays behind content, so artwork-specific backdrops such as Quick picks keep their own
 * colour and blur treatment instead of being blurred together with the page background.
 */
@Composable
fun ExperimentalThemeBackdrop(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Canvas(modifier = modifier) {
        drawRect(colors.surface)
        drawRect(
            Brush.radialGradient(
                colors = listOf(colors.primary.copy(alpha = 0.20f), Color.Transparent),
                center = Offset(size.width * 0.18f, size.height * 0.12f),
                radius = size.maxDimension * 0.72f,
                tileMode = TileMode.Clamp,
            ),
        )
        drawRect(
            Brush.radialGradient(
                colors = listOf(colors.secondary.copy(alpha = 0.16f), Color.Transparent),
                center = Offset(size.width * 0.86f, size.height * 0.30f),
                radius = size.maxDimension * 0.68f,
                tileMode = TileMode.Clamp,
            ),
        )
        drawRect(
            Brush.radialGradient(
                colors = listOf(colors.tertiary.copy(alpha = 0.11f), Color.Transparent),
                center = Offset(size.width * 0.48f, size.height * 0.92f),
                radius = size.maxDimension * 0.62f,
                tileMode = TileMode.Clamp,
            ),
        )
    }
}
