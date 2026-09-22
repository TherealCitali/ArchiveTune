/*
 * LunarTune (2026)
 * © cognitiveshadows03 — github.com/cognitiveshadows03
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.citali.lunartune.ui.experimental

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.booleanPreferencesKey
import dev.citali.lunartune.R
import dev.citali.lunartune.ui.theme.LunarMotion
import moe.rukamori.archivetune.innertube.pages.HomePage

/**
 * Master switch for the experimental home redesign. Everything under
 * [dev.citali.lunartune.ui.experimental] is gated behind this flag and must keep
 * working on Android 10 (API 29): canvas gradients, shapes and springs only —
 * no blur, no RenderEffect, no platform-gated APIs.
 */
val ExperimentalUiEnabledKey = booleanPreferencesKey("experimentalUiEnabled")

/**
 * Aurora backdrop: three overlapping radial color blobs melting into the
 * background. Pure canvas gradients, so it renders identically on API 29.
 */
@Composable
fun ExperimentalHomeBackdrop(modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val background = MaterialTheme.colorScheme.background
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(560.dp)
                .drawWithCache {
                    val w = size.width
                    val h = size.height
                    val blobA =
                        Brush.radialGradient(
                            0f to primary.copy(alpha = 0.30f),
                            0.65f to primary.copy(alpha = 0.07f),
                            1f to Color.Transparent,
                            center = Offset(w * 0.10f, h * 0.16f),
                            radius = w * 0.90f,
                        )
                    val blobB =
                        Brush.radialGradient(
                            0f to secondary.copy(alpha = 0.24f),
                            0.65f to secondary.copy(alpha = 0.06f),
                            1f to Color.Transparent,
                            center = Offset(w * 0.95f, h * 0.10f),
                            radius = w * 0.80f,
                        )
                    val blobC =
                        Brush.radialGradient(
                            0f to tertiary.copy(alpha = 0.20f),
                            0.65f to tertiary.copy(alpha = 0.05f),
                            1f to Color.Transparent,
                            center = Offset(w * 0.45f, h * 0.42f),
                            radius = w * 0.95f,
                        )
                    val fade =
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.55f to Color.Transparent,
                            1f to background,
                        )
                    onDrawBehind {
                        drawRect(blobA)
                        drawRect(blobB)
                        drawRect(blobC)
                        drawRect(fade)
                    }
                },
    )
}

/**
 * Editorial section header: overline kicker + bold display title + circular
 * arrow action.
 */
@Composable
fun ExperimentalSectionHeader(
    title: String,
    kicker: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier =
            modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 20.dp, vertical = 10.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = kicker,
                style =
                    MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (onClick != null) {
            Surface(
                onClick = onClick,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.arrow_forward),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

/**
 * Bouncy pill chips replacing the filter chips on the experimental home.
 */
@Composable
fun ExperimentalCategoryChips(
    chips: List<HomePage.Chip>,
    selectedChip: HomePage.Chip?,
    onChipSelected: (HomePage.Chip) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier =
            modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        chips.forEach { chip ->
            val selected = chip == selectedChip
            val scale by animateFloatAsState(
                targetValue = if (selected) 1.06f else 1f,
                animationSpec = LunarMotion.bouncy(),
                label = "experimentalChipScale",
            )
            Surface(
                onClick = { onChipSelected(chip) },
                shape = CircleShape,
                color =
                    if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                contentColor =
                    if (selected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                border =
                    if (selected) {
                        null
                    } else {
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    },
                modifier =
                    Modifier.graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    },
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    if (selected) {
                        Icon(
                            painter = painterResource(R.drawable.done),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Text(
                        text = chip.title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
