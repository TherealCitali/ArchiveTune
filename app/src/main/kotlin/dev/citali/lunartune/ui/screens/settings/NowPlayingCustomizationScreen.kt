/*
 * LunarTune (2026)
 * © cognitiveshadows03 — github.com/cognitiveshadows03
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package dev.citali.lunartune.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import dev.citali.lunartune.LocalPlayerAwareWindowInsets
import dev.citali.lunartune.R
import dev.citali.lunartune.constants.HidePlayerThumbnailKey
import dev.citali.lunartune.constants.PlayerBackgroundStyle
import dev.citali.lunartune.constants.PlayerBackgroundStyleKey
import dev.citali.lunartune.constants.PlayerButtonsStyle
import dev.citali.lunartune.constants.PlayerButtonsStyleKey
import dev.citali.lunartune.constants.PlayerDesignStyle
import dev.citali.lunartune.constants.PlayerDesignStyleKey
import dev.citali.lunartune.constants.ShowPlayerVolumeBarKey
import dev.citali.lunartune.constants.SliderStyle
import dev.citali.lunartune.constants.SliderStyleKey
import dev.citali.lunartune.models.MediaMetadata
import dev.citali.lunartune.ui.component.DefaultDialog
import dev.citali.lunartune.ui.component.EnumListPreference
import dev.citali.lunartune.ui.component.IconButton
import dev.citali.lunartune.ui.component.PreferenceEntry
import dev.citali.lunartune.ui.component.PreferenceGroup
import dev.citali.lunartune.ui.component.SwitchPreference
import dev.citali.lunartune.ui.player.PlayerBackground
import dev.citali.lunartune.ui.player.StyledPlaybackSlider
import dev.citali.lunartune.ui.utils.backToMain
import dev.citali.lunartune.utils.rememberEnumPreference
import dev.citali.lunartune.utils.rememberPreference

internal const val NowPlayingPreviewVideoId = "Xs0Lxif1u9E"
internal const val NowPlayingPreviewArtworkUrl =
    "https://i.ytimg.com/vi/$NowPlayingPreviewVideoId/hq720.jpg"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingCustomizationScreen(navController: NavController) {
    val (playerDesignStyle, onPlayerDesignStyleChange) =
        rememberEnumPreference(PlayerDesignStyleKey, PlayerDesignStyle.V4)
    val (showPlayerVolumeBar, onShowPlayerVolumeBarChange) =
        rememberPreference(ShowPlayerVolumeBarKey, defaultValue = true)
    val (hidePlayerThumbnail, onHidePlayerThumbnailChange) =
        rememberPreference(HidePlayerThumbnailKey, defaultValue = false)
    val (playerBackground, onPlayerBackgroundChange) =
        rememberEnumPreference(PlayerBackgroundStyleKey, PlayerBackgroundStyle.DEFAULT)
    val (playerButtonsStyle, onPlayerButtonsStyleChange) =
        rememberEnumPreference(PlayerButtonsStyleKey, PlayerButtonsStyle.DEFAULT)
    val (sliderStyle, onSliderStyleChange) =
        rememberEnumPreference(SliderStyleKey, SliderStyle.Standard)
    val (playerCustomImageUri) = rememberPreference(dev.citali.lunartune.constants.PlayerCustomImageUriKey, "")
    val (playerCustomBlur) = rememberPreference(dev.citali.lunartune.constants.PlayerCustomBlurKey, 0f)
    val (playerCustomContrast) = rememberPreference(dev.citali.lunartune.constants.PlayerCustomContrastKey, 1f)
    val (playerCustomBrightness) = rememberPreference(dev.citali.lunartune.constants.PlayerCustomBrightnessKey, 1f)
    val (disableBlur) = rememberPreference(dev.citali.lunartune.constants.DisableBlurKey, false)
    val (blurRadius) = rememberPreference(dev.citali.lunartune.constants.BlurRadiusKey, 48f)

    val isPlayerBackgroundCustomizationEnabled =
        when (playerDesignStyle) {
            PlayerDesignStyle.V7,
            PlayerDesignStyle.V7_LEGACY,
            PlayerDesignStyle.V8,
            PlayerDesignStyle.V9,
            -> false
            else -> true
        }
    val isPlayerControlsCustomizationEnabled =
        playerDesignStyle == PlayerDesignStyle.V7_LEGACY || isPlayerBackgroundCustomizationEnabled
    val isVolumeBarSupported =
        playerDesignStyle == PlayerDesignStyle.V7 || playerDesignStyle == PlayerDesignStyle.V8

    var showSliderOptionDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(isPlayerBackgroundCustomizationEnabled, playerBackground) {
        if (!isPlayerBackgroundCustomizationEnabled && playerBackground != PlayerBackgroundStyle.DEFAULT) {
            onPlayerBackgroundChange(PlayerBackgroundStyle.DEFAULT)
        }
    }
    LaunchedEffect(isPlayerControlsCustomizationEnabled) {
        if (!isPlayerControlsCustomizationEnabled) showSliderOptionDialog = false
    }

    if (showSliderOptionDialog && isPlayerControlsCustomizationEnabled) {
        val sliderStyles =
            remember {
                listOf(
                    SliderStyle.Standard,
                    SliderStyle.Wavy,
                    SliderStyle.Thick,
                    SliderStyle.Circular,
                    SliderStyle.Simple,
                )
            }
        DefaultDialog(
            buttons = {
                TextButton(
                    onClick = { showSliderOptionDialog = false },
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }
            },
            onDismiss = { showSliderOptionDialog = false },
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                sliderStyles.chunked(3).forEach { styleRow ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        styleRow.forEach { style ->
                            NowPlayingSliderStyleCard(
                                sliderStyle = style,
                                selected = sliderStyle == style,
                                onClick = {
                                    onSliderStyleChange(style)
                                    showSliderOptionDialog = false
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(3 - styleRow.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.customise_now_playing)) },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain,
                    ) {
                        Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            Modifier
                .padding(top = innerPadding.calculateTopPadding())
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
                ).verticalScroll(rememberScrollState())
                .padding(bottom = SettingsDimensions.ScreenBottomPadding),
        ) {
            NowPlayingPreviewCard(
                playerDesignStyle = playerDesignStyle,
                playerBackground = playerBackground,
                hideThumbnail = hidePlayerThumbnail,
                sliderStyle = sliderStyle,
                showVolumeBar = showPlayerVolumeBar && isVolumeBarSupported,
                disableBlur = disableBlur,
                blurRadius = blurRadius,
                playerCustomImageUri = playerCustomImageUri,
                playerCustomBlur = playerCustomBlur,
                playerCustomContrast = playerCustomContrast,
                playerCustomBrightness = playerCustomBrightness,
                modifier =
                    Modifier
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                        .fillMaxWidth(),
            )

            PreferenceGroup(title = stringResource(R.string.player)) {
                item {
                    EnumListPreference(
                        title = { Text(stringResource(R.string.player_design_style)) },
                        icon = { Icon(painterResource(R.drawable.palette), null) },
                        selectedValue = playerDesignStyle,
                        onValueSelected = onPlayerDesignStyleChange,
                        valueText = { it.designLabel() },
                    )
                }
                item {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.show_player_volume_bar)) },
                        description =
                            if (isVolumeBarSupported) {
                                null
                            } else {
                                stringResource(R.string.player_volume_bar_v7_v8_only)
                            },
                        icon = { Icon(painterResource(R.drawable.volume_up), null) },
                        checked = showPlayerVolumeBar,
                        onCheckedChange = onShowPlayerVolumeBarChange,
                        isEnabled = isVolumeBarSupported,
                    )
                }
                item {
                    EnumListPreference(
                        title = { Text(stringResource(R.string.player_background_style)) },
                        description =
                            if (isPlayerBackgroundCustomizationEnabled) {
                                null
                            } else {
                                stringResource(R.string.player_background_style_v8_v9_desc)
                            },
                        icon = { Icon(painterResource(R.drawable.gradient), null) },
                        selectedValue = playerBackground,
                        onValueSelected = onPlayerBackgroundChange,
                        isEnabled = isPlayerBackgroundCustomizationEnabled,
                        valueText = { it.backgroundLabel() },
                    )
                }
                item(visible = playerBackground == PlayerBackgroundStyle.CUSTOM) {
                    PreferenceEntry(
                        title = { Text(stringResource(R.string.customized_background)) },
                        icon = { Icon(painterResource(R.drawable.image), null) },
                        onClick = { navController.navigate("customize_background") },
                    )
                }
                item {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.hide_player_thumbnail)) },
                        description = stringResource(R.string.hide_player_thumbnail_desc),
                        icon = { Icon(painterResource(R.drawable.hide_image), null) },
                        checked = hidePlayerThumbnail,
                        onCheckedChange = onHidePlayerThumbnailChange,
                    )
                }
                item {
                    EnumListPreference(
                        title = { Text(stringResource(R.string.player_buttons_style)) },
                        description =
                            if (isPlayerControlsCustomizationEnabled) {
                                null
                            } else {
                                stringResource(R.string.player_background_style_v8_v9_desc)
                            },
                        icon = { Icon(painterResource(R.drawable.palette), null) },
                        selectedValue = playerButtonsStyle,
                        onValueSelected = onPlayerButtonsStyleChange,
                        isEnabled = isPlayerControlsCustomizationEnabled,
                        valueText = {
                            when (it) {
                                PlayerButtonsStyle.DEFAULT -> stringResource(R.string.default_style)
                                PlayerButtonsStyle.SECONDARY -> stringResource(R.string.secondary_color_style)
                            }
                        },
                    )
                }
                item {
                    PreferenceEntry(
                        title = { Text(stringResource(R.string.player_slider_style)) },
                        description = sliderStylePreviewLabel(sliderStyle),
                        icon = { Icon(painterResource(R.drawable.sliders), null) },
                        onClick = { showSliderOptionDialog = true },
                        isEnabled = isPlayerControlsCustomizationEnabled,
                    )
                }
            }
        }
    }
}

@Composable
private fun NowPlayingPreviewCard(
    playerDesignStyle: PlayerDesignStyle,
    playerBackground: PlayerBackgroundStyle,
    hideThumbnail: Boolean,
    sliderStyle: SliderStyle,
    showVolumeBar: Boolean,
    disableBlur: Boolean,
    blurRadius: Float,
    playerCustomImageUri: String,
    playerCustomBlur: Float,
    playerCustomContrast: Float,
    playerCustomBrightness: Float,
    modifier: Modifier = Modifier,
) {
    val previewMetadata =
        remember {
            MediaMetadata(
                id = NowPlayingPreviewVideoId,
                title = "Suzume",
                artists = listOf(MediaMetadata.Artist(id = null, name = "RADWIMPS")),
                duration = 237,
                thumbnailUrl = NowPlayingPreviewArtworkUrl,
                album = MediaMetadata.Album(id = NowPlayingPreviewVideoId, title = "Suzume"),
            )
        }
    val textColor =
        when {
            playerDesignStyle == PlayerDesignStyle.V5 -> MaterialTheme.colorScheme.onPrimaryContainer
            playerDesignStyle == PlayerDesignStyle.V9 -> MaterialTheme.colorScheme.onSurface
            playerBackground == PlayerBackgroundStyle.DEFAULT &&
                playerDesignStyle != PlayerDesignStyle.V7 &&
                playerDesignStyle != PlayerDesignStyle.V7_LEGACY &&
                playerDesignStyle != PlayerDesignStyle.V8 -> MaterialTheme.colorScheme.onSurface
            else -> Color.White
        }

    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.now_playing_preview_caption),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.62f)
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            if (playerDesignStyle == PlayerDesignStyle.V5) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer))
            } else if (
                playerDesignStyle == PlayerDesignStyle.V7 ||
                playerDesignStyle == PlayerDesignStyle.V7_LEGACY ||
                playerDesignStyle == PlayerDesignStyle.V8
            ) {
                PlayerBackground(
                    playerBackground = PlayerBackgroundStyle.BLUR,
                    mediaMetadata = previewMetadata,
                    gradientColors = emptyList(),
                    disableBlur = disableBlur,
                    blurRadius = blurRadius.coerceAtLeast(24f),
                    playerCustomImageUri = playerCustomImageUri,
                    playerCustomBlur = playerCustomBlur,
                    playerCustomContrast = playerCustomContrast,
                    playerCustomBrightness = playerCustomBrightness,
                )
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)))
            } else {
                PlayerBackground(
                    playerBackground = playerBackground,
                    mediaMetadata = previewMetadata,
                    gradientColors = listOf(Color(0xFF5C3A1E), Color(0xFF2A1A0C), Color.Black),
                    disableBlur = disableBlur,
                    blurRadius = blurRadius,
                    playerCustomImageUri = playerCustomImageUri,
                    playerCustomBlur = playerCustomBlur,
                    playerCustomContrast = playerCustomContrast,
                    playerCustomBrightness = playerCustomBrightness,
                )
            }

            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = playerDesignStyle.designLabel(),
                    style = MaterialTheme.typography.labelMedium,
                    color = textColor.copy(alpha = 0.7f),
                )
                Spacer(Modifier.height(12.dp))
                if (!hideThumbnail && playerDesignStyle != PlayerDesignStyle.V5) {
                    AsyncImage(
                        model = NowPlayingPreviewArtworkUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier =
                            Modifier
                                .size(if (playerDesignStyle == PlayerDesignStyle.V7 || playerDesignStyle == PlayerDesignStyle.V7_LEGACY) 96.dp else 140.dp)
                                .clip(RoundedCornerShape(if (playerDesignStyle == PlayerDesignStyle.V9) 28.dp else 16.dp)),
                    )
                    Spacer(Modifier.height(12.dp))
                }
                Text(
                    text = previewMetadata.title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = previewMetadata.artists.joinToString { it.name },
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor.copy(alpha = 0.72f),
                    maxLines = 1,
                )
                Spacer(Modifier.height(8.dp))
                StyledPlaybackSlider(
                    sliderStyle = sliderStyle,
                    value = 0.38f,
                    valueRange = 0f..1f,
                    onValueChange = {},
                    onValueChangeFinished = {},
                    activeColor = textColor,
                    isPlaying = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.weight(1f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(painterResource(R.drawable.skip_previous), null, tint = textColor, modifier = Modifier.size(28.dp))
                    Box(
                        modifier =
                            Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(textColor.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(painterResource(R.drawable.pause), null, tint = textColor, modifier = Modifier.size(32.dp))
                    }
                    Icon(painterResource(R.drawable.skip_next), null, tint = textColor, modifier = Modifier.size(28.dp))
                }
                if (showVolumeBar) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Icon(painterResource(R.drawable.volume_off), null, tint = textColor.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                        StyledPlaybackSlider(
                            sliderStyle = SliderStyle.Simple,
                            value = 0.7f,
                            valueRange = 0f..1f,
                            onValueChange = {},
                            onValueChangeFinished = {},
                            activeColor = textColor,
                            isPlaying = false,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        )
                        Icon(painterResource(R.drawable.volume_up), null, tint = textColor.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun NowPlayingSliderStyleCard(
    sliderStyle: SliderStyle,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var sliderValue by remember { mutableFloatStateOf(0.5f) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier =
            modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .border(
                    1.dp,
                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    RoundedCornerShape(16.dp),
                ).clickable(onClick = onClick)
                .padding(16.dp),
    ) {
        StyledPlaybackSlider(
            sliderStyle = sliderStyle,
            value = sliderValue,
            valueRange = 0f..1f,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = {},
            activeColor = MaterialTheme.colorScheme.primary,
            isPlaying = true,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        )
        Text(text = sliderStylePreviewLabel(sliderStyle), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun PlayerDesignStyle.designLabel(): String =
    when (this) {
        PlayerDesignStyle.V1 -> stringResource(R.string.player_design_v1)
        PlayerDesignStyle.V2 -> stringResource(R.string.player_design_v2)
        PlayerDesignStyle.V3 -> stringResource(R.string.player_design_v3)
        PlayerDesignStyle.V4 -> stringResource(R.string.player_design_v4)
        PlayerDesignStyle.V5 -> stringResource(R.string.player_design_v5)
        PlayerDesignStyle.V6 -> stringResource(R.string.player_design_v6)
        PlayerDesignStyle.V7 -> stringResource(R.string.player_design_v7)
                        PlayerDesignStyle.V7_LEGACY -> stringResource(R.string.player_design_v7_legacy)
                        PlayerDesignStyle.V8 -> stringResource(R.string.player_design_v8)
                        PlayerDesignStyle.V9 -> stringResource(R.string.player_design_v9)
                    }

@Composable
private fun PlayerBackgroundStyle.backgroundLabel(): String =
    when (this) {
        PlayerBackgroundStyle.DEFAULT -> stringResource(R.string.follow_theme)
        PlayerBackgroundStyle.GRADIENT -> stringResource(R.string.gradient)
        PlayerBackgroundStyle.CUSTOM -> stringResource(R.string.custom)
        PlayerBackgroundStyle.BLUR -> stringResource(R.string.player_background_blur)
        PlayerBackgroundStyle.COLORING -> stringResource(R.string.coloring)
        PlayerBackgroundStyle.BLUR_GRADIENT -> stringResource(R.string.blur_gradient)
        PlayerBackgroundStyle.GLOW -> stringResource(R.string.glow)
        PlayerBackgroundStyle.GLOW_ANIMATED -> "Glow Animated"
    }

@Composable
private fun sliderStylePreviewLabel(sliderStyle: SliderStyle): String =
    when (sliderStyle) {
        SliderStyle.Standard -> stringResource(R.string.slider_style_standard)
        SliderStyle.Wavy -> stringResource(R.string.slider_style_wavy)
        SliderStyle.Thick -> stringResource(R.string.slider_style_thick)
        SliderStyle.Circular -> stringResource(R.string.slider_style_circular)
        SliderStyle.Simple -> stringResource(R.string.slider_style_simple)
    }
