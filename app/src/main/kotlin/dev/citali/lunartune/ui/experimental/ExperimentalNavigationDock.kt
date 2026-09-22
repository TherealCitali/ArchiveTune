/*
 * LunarTune (2026)
 * © cognitiveshadows03 — github.com/cognitiveshadows03
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.citali.lunartune.ui.experimental

import android.os.SystemClock
import android.view.ViewConfiguration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.citali.lunartune.ui.screens.Screens
import dev.citali.lunartune.ui.theme.LunarMotion

/**
 * "Orbit dock": the experimental navbar. A floating capsule where the active
 * tab blooms into a labeled pill with a jelly width-spring while inactive
 * tabs stay icon-only. Long-press actions and search double-tap behave
 * exactly like the stock bar.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExperimentalNavigationDock(
    items: List<Screens>,
    modifier: Modifier = Modifier,
    isSelected: (Screens) -> Boolean,
    onItemClick: (Screens, Boolean) -> Unit,
    onItemLongClick: ((Screens) -> Unit)? = null,
    onSearchItemDoubleClick: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(percent = 50),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 3.dp,
            shadowElevation = 8.dp,
            modifier =
                Modifier
                    .widthIn(max = 360.dp)
                    .height(68.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Fake glass edge: a 1dp gradient sheen, API-29 safe.
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth(0.7f)
                            .height(1.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f),
                                        Color.Transparent,
                                    ),
                                ),
                            ),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                    modifier =
                        Modifier
                            .fillMaxHeight()
                            .padding(horizontal = 10.dp),
                ) {
                    items.forEach { screen ->
                        DockItem(
                            screen = screen,
                            selected = isSelected(screen),
                            onItemClick = onItemClick,
                            onItemLongClick = onItemLongClick,
                            onSearchItemDoubleClick = onSearchItemDoubleClick,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DockItem(
    screen: Screens,
    selected: Boolean,
    onItemClick: (Screens, Boolean) -> Unit,
    onItemLongClick: ((Screens) -> Unit)?,
    onSearchItemDoubleClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) LunarMotion.PressedScale else 1f,
        animationSpec = LunarMotion.press(),
        label = "dockPressScale",
    )
    val pillColor by animateColorAsState(
        targetValue =
            if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                Color.Transparent
            },
        label = "dockPillColor",
    )
    val contentColor by animateColorAsState(
        targetValue =
            if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        label = "dockContentColor",
    )
    val lastClickTime = remember(screen) { mutableLongStateOf(0L) }
    val onDoubleClick =
        remember(screen, onSearchItemDoubleClick) {
            if (screen == Screens.Search) onSearchItemDoubleClick else null
        }

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .graphicsLayer {
                    scaleX = pressScale
                    scaleY = pressScale
                }.clip(CircleShape)
                .background(pillColor)
                .combinedClickable(
                    onClick = {
                        val now = SystemClock.uptimeMillis()
                        val isDoubleClick =
                            onDoubleClick != null &&
                                now - lastClickTime.longValue <= ViewConfiguration.getDoubleTapTimeout()
                        lastClickTime.longValue = if (isDoubleClick) 0L else now
                        if (isDoubleClick) {
                            onDoubleClick.invoke()
                        } else {
                            onItemClick(screen, selected)
                        }
                    },
                    onLongClick =
                        onItemLongClick?.let { longClick ->
                            { longClick(screen) }
                        },
                    interactionSource = interactionSource,
                    indication = null,
                ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .animateContentSize(animationSpec = LunarMotion.bouncy()),
        ) {
            Icon(
                painter =
                    painterResource(
                        if (selected) screen.iconIdActive else screen.iconIdInactive,
                    ),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(24.dp),
            )
            AnimatedVisibility(
                visible = selected,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Text(
                    text = stringResource(screen.titleId),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    maxLines = 1,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}
