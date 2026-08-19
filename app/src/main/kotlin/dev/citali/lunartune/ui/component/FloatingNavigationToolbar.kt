/*
 * LunarTune (2026)
 * © cognitiveshadows03 — github.com/cognitiveshadows03
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package dev.citali.lunartune.ui.component

import android.os.SystemClock
import android.view.ViewConfiguration
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarArrangement
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.ShortNavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.citali.lunartune.constants.FloatingNavigationBarMaxWidth
import dev.citali.lunartune.constants.HideNavigationBarLabelsKey
import dev.citali.lunartune.constants.NAVIGATION_BAR_CORNER_RADIUS_DEFAULT
import dev.citali.lunartune.constants.NAVIGATION_BAR_HEIGHT_DEFAULT
import dev.citali.lunartune.constants.NAVIGATION_BAR_LABEL_SPACING_DEFAULT
import dev.citali.lunartune.constants.NAVIGATION_BAR_OPACITY_DEFAULT
import dev.citali.lunartune.constants.NAVIGATION_BAR_TRANSPARENCY_DEFAULT
import dev.citali.lunartune.constants.NAVIGATION_BAR_WIDTH_DEFAULT
import dev.citali.lunartune.constants.NavigationBarCornerRadiusKey
import dev.citali.lunartune.constants.NavigationBarHeight
import dev.citali.lunartune.constants.NavigationBarHeightKey
import dev.citali.lunartune.constants.NavigationBarLabelSpacingKey
import dev.citali.lunartune.constants.NavigationBarMaxWidth
import dev.citali.lunartune.constants.NavigationBarOpacityKey
import dev.citali.lunartune.constants.NavigationBarStyle
import dev.citali.lunartune.constants.NavigationBarTransparencyKey
import dev.citali.lunartune.constants.NavigationBarWidthKey
import dev.citali.lunartune.ui.screens.Screens
import dev.citali.lunartune.utils.rememberPreference
import kotlin.math.roundToInt

private val NavigationItemsMaxWidth = 360.dp
private val NavigationItemVerticalPadding = 8.dp
private val NavigationIndicatorWidth = 56.dp
private val NavigationIndicatorHeight = 32.dp
private val FloatingNavigationIndicatorWidth = 64.dp
private val FloatingNavigationIndicatorHeight = 42.dp

@Composable
fun FloatingNavigationToolbar(
    items: List<Screens>,
    pureBlack: Boolean,
    modifier: Modifier = Modifier,
    isPairedWithMiniPlayer: Boolean = false,
    style: NavigationBarStyle = NavigationBarStyle.DEFAULT,
    isSelected: (Screens) -> Boolean,
    onItemClick: (Screens, Boolean) -> Unit,
    onSearchItemDoubleClick: (() -> Unit)? = null,
) {
    val isFloating = style == NavigationBarStyle.FLOATING
    val (navBarWidthFraction) =
        rememberPreference(NavigationBarWidthKey, defaultValue = NAVIGATION_BAR_WIDTH_DEFAULT)
    val (navBarHeightMultiplier) =
        rememberPreference(NavigationBarHeightKey, defaultValue = NAVIGATION_BAR_HEIGHT_DEFAULT)
    val (navBarOpacity) =
        rememberPreference(NavigationBarOpacityKey, defaultValue = NAVIGATION_BAR_OPACITY_DEFAULT)
    val (navBarTransparency) =
        rememberPreference(NavigationBarTransparencyKey, defaultValue = NAVIGATION_BAR_TRANSPARENCY_DEFAULT)
    val (navBarLabelSpacing) =
        rememberPreference(NavigationBarLabelSpacingKey, defaultValue = NAVIGATION_BAR_LABEL_SPACING_DEFAULT)
    val (navBarCornerRadius) =
        rememberPreference(NavigationBarCornerRadiusKey, defaultValue = NAVIGATION_BAR_CORNER_RADIUS_DEFAULT)
    val (hideNavigationLabels) = rememberPreference(HideNavigationBarLabelsKey, defaultValue = false)
    val resolvedBarHeight = NavigationBarHeight * navBarHeightMultiplier
    val navigationShape =
        remember(isPairedWithMiniPlayer, isFloating, navBarCornerRadius) {
            when {
                isFloating -> RoundedCornerShape(navBarCornerRadius.dp)
                isPairedWithMiniPlayer ->
                    RoundedCornerShape(
                        topStart = 12.dp,
                        topEnd = 12.dp,
                        bottomStart = navBarCornerRadius.dp,
                        bottomEnd = navBarCornerRadius.dp,
                    )
                else -> null
            }
        } ?: MaterialTheme.shapes.extraLarge
    val navigationContainerColor =
        if (pureBlack) {
            Color.Black
        } else {
            val effectiveAlpha = navBarOpacity * (1f - navBarTransparency)
            MaterialTheme.colorScheme.surfaceContainer.copy(alpha = effectiveAlpha.coerceIn(0.05f, 1f))
        }
    val motionScheme = MaterialTheme.motionScheme
    val density = LocalDensity.current
    val indicatorColor =
        when {
            isFloating -> MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)
            pureBlack -> Color.White.copy(alpha = 0.16f)
            else -> MaterialTheme.colorScheme.secondaryContainer
        }
    val indicatorWidth = if (isFloating) FloatingNavigationIndicatorWidth else NavigationIndicatorWidth
    val indicatorHeight = if (isFloating) FloatingNavigationIndicatorHeight else NavigationIndicatorHeight
    val itemColors =
        if (isFloating) {
            ShortNavigationBarItemDefaults.colors(
                selectedIndicatorColor = Color.Transparent,
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor =
                    if (pureBlack) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor =
                    if (pureBlack) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else if (pureBlack) {
            ShortNavigationBarItemDefaults.colors(
                selectedIndicatorColor = Color.Transparent,
                selectedIconColor = Color.White,
                selectedTextColor = Color.White,
                unselectedIconColor = Color.White.copy(alpha = 0.6f),
                unselectedTextColor = Color.White.copy(alpha = 0.6f),
            )
        } else {
            ShortNavigationBarItemDefaults.colors(selectedIndicatorColor = Color.Transparent)
        }

    val selectedIndex = items.indexOfFirst { isSelected(it) }
    val iconCenters = remember { mutableStateMapOf<Int, Offset>() }
    var containerPos by remember { mutableStateOf(Offset.Zero) }
    val indicatorX = remember { Animatable(0f) }
    var indicatorY by remember { mutableFloatStateOf(0f) }
    var indicatorPlaced by remember { mutableStateOf(false) }
    val selectedCenter = if (selectedIndex >= 0) iconCenters[selectedIndex] else null

    LaunchedEffect(selectedIndex, selectedCenter, containerPos, indicatorWidth, indicatorHeight) {
        val center = selectedCenter ?: return@LaunchedEffect
        val widthPx = with(density) { indicatorWidth.toPx() }
        val heightPx = with(density) { indicatorHeight.toPx() }
        val targetX = (center.x - containerPos.x) - widthPx / 2f
        indicatorY = (center.y - containerPos.y) - heightPx / 2f
        if (!indicatorPlaced) {
            indicatorX.snapTo(targetX)
            indicatorPlaced = true
        } else {
            indicatorX.animateTo(
                targetValue = targetX,
                animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
            )
        }
    }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier =
                Modifier
                    .widthIn(max = if (isFloating) FloatingNavigationBarMaxWidth else NavigationBarMaxWidth)
                    .fillMaxWidth(if (isFloating) navBarWidthFraction.coerceIn(0.5f, 1f) else 1f)
                    .height(resolvedBarHeight),
            shape = navigationShape,
            color = navigationContainerColor,
            tonalElevation = NavigationBarDefaults.Elevation,
            shadowElevation = if (isFloating) 8.dp else NavigationBarDefaults.Elevation,
        ) {
            ShortNavigationBar(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,
                contentColor = if (pureBlack) Color.White else MaterialTheme.colorScheme.onSurface,
                windowInsets = WindowInsets(0, 0, 0, 0),
                arrangement = ShortNavigationBarArrangement.EqualWeight,
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .onGloballyPositioned { containerPos = it.positionInRoot() },
                    contentAlignment = Alignment.Center,
                ) {
                    if (selectedIndex >= 0 && indicatorPlaced) {
                        Box(
                            modifier =
                                Modifier
                                    .align(Alignment.TopStart)
                                    .offset {
                                        IntOffset(
                                            indicatorX.value.roundToInt(),
                                            indicatorY.roundToInt(),
                                        )
                                    }.width(indicatorWidth)
                                    .height(indicatorHeight)
                                    .clip(RoundedCornerShape(percent = 50))
                                    .background(indicatorColor),
                        )
                    }
                    Row(
                        modifier =
                            Modifier
                                .widthIn(max = NavigationItemsMaxWidth)
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .padding(vertical = NavigationItemVerticalPadding),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        items.forEachIndexed { index, screen ->
                            val selected = isSelected(screen)
                            val iconScale = remember(screen) { Animatable(1f) }
                            LaunchedEffect(selected) {
                                if (selected) {
                                    iconScale.snapTo(0.85f)
                                    iconScale.animateTo(
                                        1f,
                                        spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMediumLow,
                                        ),
                                    )
                                } else {
                                    iconScale.snapTo(1f)
                                }
                            }
                            val onDoubleClick =
                                remember(screen, onSearchItemDoubleClick) {
                                    if (screen == Screens.Search) onSearchItemDoubleClick else null
                                }
                            val lastClickTime = remember(screen) { mutableLongStateOf(0L) }
                            var ignoreNextClick by remember(screen) { mutableStateOf(false) }
                            val onClick =
                                remember(screen, selected, onItemClick, onDoubleClick) {
                                    {
                                        if (ignoreNextClick) {
                                            ignoreNextClick = false
                                        } else {
                                            val currentTime = SystemClock.uptimeMillis()
                                            val isDoubleClick =
                                                onDoubleClick != null &&
                                                    currentTime - lastClickTime.longValue <= ViewConfiguration.getDoubleTapTimeout()
                                            lastClickTime.longValue = if (isDoubleClick) 0L else currentTime
                                            if (isDoubleClick) {
                                                onDoubleClick?.invoke()
                                            } else {
                                                onItemClick(screen, selected)
                                            }
                                        }
                                    }
                                }

                            ShortNavigationBarItem(
                                selected = selected,
                                onClick = onClick,
                                colors = itemColors,
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .pointerInput(screen, onItemLongClick) {
                                            if (onItemLongClick == null) return@pointerInput
                                            detectTapGestures(
                                                onLongPress = {
                                                    ignoreNextClick = true
                                                    onItemLongClick(screen)
                                                },
                                            )
                                        },
                                icon = {
                                    Box(
                                        modifier =
                                            Modifier.onGloballyPositioned { coordinates ->
                                                val pos = coordinates.positionInRoot()
                                                iconCenters[index] =
                                                    Offset(
                                                        pos.x + coordinates.size.width / 2f,
                                                        pos.y + coordinates.size.height / 2f,
                                                    )
                                            },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Crossfade(
                                            targetState = selected,
                                            animationSpec = motionScheme.fastEffectsSpec(),
                                            label = "navigationItemIcon",
                                        ) { isSelected ->
                                            Icon(
                                                painter =
                                                    painterResource(
                                                        if (isSelected) screen.iconIdActive else screen.iconIdInactive,
                                                    ),
                                                contentDescription = null,
                                                modifier =
                                                    Modifier.graphicsLayer {
                                                        scaleX = iconScale.value
                                                        scaleY = iconScale.value
                                                    },
                                            )
                                        }
                                    }
                                },
                                label =
                                    if (hideNavigationLabels) {
                                        null
                                    } else {
                                        {
                                            Text(
                                                text = stringResource(screen.titleId),
                                                maxLines = 1,
                                            )
                                        }
                                    },
                            )
                        }
                    }
                }
            }
        }
    }
}
