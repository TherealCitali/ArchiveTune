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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import dev.citali.lunartune.LocalPlayerAwareWindowInsets
import dev.citali.lunartune.LocalPlayerConnection
import dev.citali.lunartune.R
import dev.citali.lunartune.constants.DarkModeKey
import dev.citali.lunartune.constants.HidePlayerThumbnailKey
import dev.citali.lunartune.constants.PlayerBackgroundStyle
import dev.citali.lunartune.constants.PlayerBackgroundStyleKey
import dev.citali.lunartune.constants.PlayerButtonsStyle
import dev.citali.lunartune.constants.PlayerButtonsStyleKey
import dev.citali.lunartune.constants.PlayerDesignStyle
import dev.citali.lunartune.constants.PlayerDesignStyleKey
import dev.citali.lunartune.constants.ShowPlayerVolumeBarKey
import dev.citali.lunartune.db.entities.FormatEntity
import dev.citali.lunartune.constants.SliderStyle
import dev.citali.lunartune.constants.SliderStyleKey
import dev.citali.lunartune.models.MediaMetadata
import dev.citali.lunartune.playback.PlayerConnection
import dev.citali.lunartune.ui.component.DefaultDialog
import dev.citali.lunartune.ui.component.EXPANDED_ANCHOR
import dev.citali.lunartune.ui.component.LocalBottomSheetPageState
import dev.citali.lunartune.ui.component.LocalMenuState
import dev.citali.lunartune.ui.component.rememberBottomSheetState
import dev.citali.lunartune.ui.component.EnumListPreference
import dev.citali.lunartune.ui.component.IconButton
import dev.citali.lunartune.ui.component.PreferenceEntry
import dev.citali.lunartune.ui.component.PreferenceGroup
import dev.citali.lunartune.ui.component.SwitchPreference
import dev.citali.lunartune.ui.player.LittlePlayerContent
import dev.citali.lunartune.ui.player.PlayerBackground
import dev.citali.lunartune.ui.player.PlayerControlsContent
import dev.citali.lunartune.ui.player.V8PlayerContent
import dev.citali.lunartune.ui.player.V8PlayerControlsContent
import dev.citali.lunartune.ui.player.V9PlayerContent
import dev.citali.lunartune.ui.player.rememberDeviceMusicVolumeController
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
                navController = navController,
                playerDesignStyle = playerDesignStyle,
                playerBackground = playerBackground,
                playerButtonsStyle = playerButtonsStyle,
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

private const val PreviewPositionMs = 90_000L
private const val PreviewDurationMs = 237_000L

private val PreviewGradientColors =
    listOf(Color(0xFF5C3A1E), Color(0xFF2A1A0C), Color.Black)

private class PreviewPlaybackState(
    val playbackState: Int,
    val isPlaying: Boolean,
    val isLoading: Boolean,
    val repeatMode: Int,
    val canSkipPrevious: Boolean,
    val canSkipNext: Boolean,
    val currentFormat: FormatEntity?,
    val liked: Boolean,
)

@Composable
private fun rememberPreviewPlaybackState(connection: PlayerConnection?): PreviewPlaybackState {
    if (connection == null) {
        return PreviewPlaybackState(
            playbackState = Player.STATE_READY,
            isPlaying = true,
            isLoading = false,
            repeatMode = Player.REPEAT_MODE_OFF,
            canSkipPrevious = true,
            canSkipNext = true,
            currentFormat = null,
            liked = false,
        )
    }
    val playbackState by connection.playbackState.collectAsState()
    val isPlaying by connection.isPlaying.collectAsState()
    val repeatMode by connection.repeatMode.collectAsState()
    val canSkipPrevious by connection.canSkipPrevious.collectAsState()
    val canSkipNext by connection.canSkipNext.collectAsState()
    val currentFormat by connection.currentFormat.collectAsState(initial = null)
    val currentSong by connection.currentSong.collectAsState(initial = null)
    return PreviewPlaybackState(
        playbackState = playbackState,
        isPlaying = isPlaying,
        isLoading = playbackState == Player.STATE_BUFFERING,
        repeatMode = repeatMode,
        canSkipPrevious = canSkipPrevious,
        canSkipNext = canSkipNext,
        currentFormat = currentFormat,
        liked = currentSong?.song?.liked == true,
    )
}

private fun previewHsvTone(
    base: Color,
    useDarkTheme: Boolean,
): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(base.toArgb(), hsv)
    if (useDarkTheme) {
        hsv[1] = hsv[1].coerceAtMost(0.12f)
        hsv[2] = 0.96f
    } else {
        hsv[1] = hsv[1].coerceIn(0.12f, 0.35f)
        hsv[2] = 0.08f
    }
    return Color(android.graphics.Color.HSVToColor(hsv))
}

@Composable
private fun PreviewBlurBackdrop(
    previewMetadata: MediaMetadata,
    disableBlur: Boolean,
    blurRadius: Float,
    playerCustomImageUri: String,
    playerCustomBlur: Float,
    playerCustomContrast: Float,
    playerCustomBrightness: Float,
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
}

@Composable
private fun StaticPreviewFallback(
    previewMetadata: MediaMetadata,
    sliderStyle: SliderStyle,
) {
    val textColor = MaterialTheme.colorScheme.onSurface
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(0.2f))
        AsyncImage(
            model = NowPlayingPreviewArtworkUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier =
                Modifier
                    .size(140.dp)
                    .clip(RoundedCornerShape(16.dp)),
        )
        Spacer(Modifier.height(12.dp))
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
    }
}

@Composable
private fun NowPlayingPreviewCard(
    navController: NavController,
    playerDesignStyle: PlayerDesignStyle,
    playerBackground: PlayerBackgroundStyle,
    playerButtonsStyle: PlayerButtonsStyle,
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
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current
    val previewSheetState =
        rememberBottomSheetState(
            dismissedBound = 0.dp,
            expandedBound = 900.dp,
            collapsedBound = 0.dp,
            initialAnchor = EXPANDED_ANCHOR,
        )
    val volumeController = rememberDeviceMusicVolumeController()
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
    val playback = rememberPreviewPlaybackState(playerConnection)

    val isSystemInDarkTheme = isSystemInDarkTheme()
    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val useDarkTheme =
        remember(darkTheme, isSystemInDarkTheme) {
            if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
        }

    val defaultTextBackgroundColor =
        when (playerBackground) {
            PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.onBackground
            else -> Color.White
        }
    val defaultIcBackgroundColor =
        when (playerBackground) {
            PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.surface
            else -> Color.Black
        }
    val (defaultTextButtonColor, defaultIconButtonColor) =
        when (playerButtonsStyle) {
            PlayerButtonsStyle.DEFAULT -> Pair(defaultTextBackgroundColor, defaultIcBackgroundColor)
            PlayerButtonsStyle.SECONDARY ->
                Pair(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.onSecondary)
        }

    val v9AccentColor = PreviewGradientColors.first()
    val v9TextColor = remember(v9AccentColor, useDarkTheme) { previewHsvTone(v9AccentColor, useDarkTheme) }
    val v9IconButtonColor =
        remember(v9AccentColor) {
            val luminance =
                0.299f * v9AccentColor.red + 0.587f * v9AccentColor.green + 0.114f * v9AccentColor.blue
            if (luminance > 0.5f) Color.Black else Color.White
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
                    .aspectRatio(0.58f)
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            val connection = playerConnection
            if (connection == null) {
                StaticPreviewFallback(previewMetadata, sliderStyle)
            } else {
                when (playerDesignStyle) {
                    PlayerDesignStyle.V5 -> {
                        val littleBackground = MaterialTheme.colorScheme.primaryContainer
                        val littleTextColor = MaterialTheme.colorScheme.onPrimaryContainer
                        val progressFraction =
                            (PreviewPositionMs.toFloat() / PreviewDurationMs.toFloat()).coerceIn(0f, 1f)
                        Box(Modifier.fillMaxSize().background(littleBackground)) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(progressFraction)
                                    .align(Alignment.TopStart)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
                            )
                            LittlePlayerContent(
                                mediaMetadata = previewMetadata,
                                sliderPosition = null,
                                positionMs = PreviewPositionMs,
                                durationMs = PreviewDurationMs,
                                textColor = littleTextColor,
                                liked = playback.liked,
                                onCollapse = {},
                                onToggleLike = { connection.toggleLike() },
                                onExpandQueue = {},
                                onMenuClick = {},
                            )
                        }
                    }

                    PlayerDesignStyle.V7_LEGACY -> {
                        PreviewBlurBackdrop(
                            previewMetadata = previewMetadata,
                            disableBlur = disableBlur,
                            blurRadius = blurRadius,
                            playerCustomImageUri = playerCustomImageUri,
                            playerCustomBlur = playerCustomBlur,
                            playerCustomContrast = playerCustomContrast,
                            playerCustomBrightness = playerCustomBrightness,
                        )
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Spacer(Modifier.weight(1f))
                            PlayerControlsContent(
                                mediaMetadata = previewMetadata,
                                playerDesignStyle = playerDesignStyle,
                                sliderStyle = sliderStyle,
                                playbackState = playback.playbackState,
                                isPlaying = playback.isPlaying,
                                isLoading = playback.isLoading,
                                repeatMode = playback.repeatMode,
                                canSkipPrevious = playback.canSkipPrevious,
                                canSkipNext = playback.canSkipNext,
                                textButtonColor = Color.White,
                                iconButtonColor = Color.Black,
                                textBackgroundColor = Color.White,
                                icBackgroundColor = Color.Black,
                                sliderPosition = null,
                                position = PreviewPositionMs,
                                duration = PreviewDurationMs,
                                playerConnection = connection,
                                navController = navController,
                                state = previewSheetState,
                                menuState = menuState,
                                bottomSheetPageState = bottomSheetPageState,
                                context = context,
                                onSliderValueChange = {},
                                onSliderValueChangeFinished = {},
                            )
                            Spacer(Modifier.height(16.dp))
                        }
                    }

                    PlayerDesignStyle.V7 -> {
                        PreviewBlurBackdrop(
                            previewMetadata = previewMetadata,
                            disableBlur = disableBlur,
                            blurRadius = blurRadius,
                            playerCustomImageUri = playerCustomImageUri,
                            playerCustomBlur = playerCustomBlur,
                            playerCustomContrast = playerCustomContrast,
                            playerCustomBrightness = playerCustomBrightness,
                        )
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Spacer(Modifier.weight(1f))
                            V8PlayerControlsContent(
                                mediaMetadata = previewMetadata,
                                queueTitle = "",
                                playbackState = playback.playbackState,
                                isPlaying = playback.isPlaying,
                                isLoading = playback.isLoading,
                                canSkipPrevious = playback.canSkipPrevious,
                                canSkipNext = playback.canSkipNext,
                                currentSongLiked = playback.liked,
                                sliderPosition = null,
                                position = PreviewPositionMs,
                                duration = PreviewDurationMs,
                                volume = volumeController.volumeFraction,
                                showVolumeBar = showVolumeBar,
                                currentFormat = playback.currentFormat,
                                playerConnection = connection,
                                navController = navController,
                                state = previewSheetState,
                                menuState = menuState,
                                bottomSheetPageState = bottomSheetPageState,
                                onSliderValueChange = {},
                                onSliderValueChangeFinished = {},
                                onVolumeChange = volumeController::setVolumeFraction,
                            )
                            Spacer(Modifier.height(16.dp))
                        }
                    }

                    PlayerDesignStyle.V8 -> {
                        PreviewBlurBackdrop(
                            previewMetadata = previewMetadata,
                            disableBlur = disableBlur,
                            blurRadius = blurRadius,
                            playerCustomImageUri = playerCustomImageUri,
                            playerCustomBlur = playerCustomBlur,
                            playerCustomContrast = playerCustomContrast,
                            playerCustomBrightness = playerCustomBrightness,
                        )
                        V8PlayerContent(
                            mediaMetadata = previewMetadata,
                            queueTitle = null,
                            playbackState = playback.playbackState,
                            isPlaying = playback.isPlaying,
                            isLoading = playback.isLoading,
                            canSkipPrevious = playback.canSkipPrevious,
                            canSkipNext = playback.canSkipNext,
                            currentSongLiked = playback.liked,
                            sliderPosition = null,
                            position = PreviewPositionMs,
                            duration = PreviewDurationMs,
                            volume = volumeController.volumeFraction,
                            showVolumeBar = showVolumeBar,
                            playerConnection = connection,
                            navController = navController,
                            state = previewSheetState,
                            menuState = menuState,
                            bottomSheetPageState = bottomSheetPageState,
                            currentFormat = playback.currentFormat,
                            canvasPrimaryUrl = null,
                            canvasFallbackUrl = null,
                            onSliderValueChange = {},
                            onSliderValueChangeFinished = {},
                            onVolumeChange = volumeController::setVolumeFraction,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    PlayerDesignStyle.V9 -> {
                        V9PlayerContent(
                            mediaMetadata = previewMetadata,
                            playbackState = playback.playbackState,
                            isPlaying = playback.isPlaying,
                            isLoading = playback.isLoading,
                            canSkipPrevious = playback.canSkipPrevious,
                            canSkipNext = playback.canSkipNext,
                            sliderPosition = null,
                            position = PreviewPositionMs,
                            duration = PreviewDurationMs,
                            playerConnection = connection,
                            navController = navController,
                            state = previewSheetState,
                            textBackgroundColor = v9TextColor,
                            textButtonColor = v9AccentColor,
                            iconButtonColor = v9IconButtonColor,
                            canvasPrimaryUrl = null,
                            canvasFallbackUrl = null,
                            onCollapseClick = {},
                            onQueueClick = {},
                            onLyricsClick = {},
                            onSliderValueChange = {},
                            onSliderValueChangeFinished = {},
                            modifier = Modifier.fillMaxSize(),
                            gradientColors = PreviewGradientColors,
                        )
                    }

                    else -> {
                        PlayerBackground(
                            playerBackground = playerBackground,
                            mediaMetadata = previewMetadata,
                            gradientColors = PreviewGradientColors,
                            disableBlur = disableBlur,
                            blurRadius = blurRadius,
                            playerCustomImageUri = playerCustomImageUri,
                            playerCustomBlur = playerCustomBlur,
                            playerCustomContrast = playerCustomContrast,
                            playerCustomBrightness = playerCustomBrightness,
                        )
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            if (!hideThumbnail) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier =
                                        Modifier
                                            .weight(1f)
                                            .fillMaxWidth(),
                                ) {
                                    AsyncImage(
                                        model = NowPlayingPreviewArtworkUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier =
                                            Modifier
                                                .fillMaxWidth(0.72f)
                                                .aspectRatio(1f)
                                                .clip(RoundedCornerShape(16.dp)),
                                    )
                                }
                            } else {
                                Spacer(Modifier.weight(1f))
                            }
                            PlayerControlsContent(
                                mediaMetadata = previewMetadata,
                                playerDesignStyle = playerDesignStyle,
                                sliderStyle = sliderStyle,
                                playbackState = playback.playbackState,
                                isPlaying = playback.isPlaying,
                                isLoading = playback.isLoading,
                                repeatMode = playback.repeatMode,
                                canSkipPrevious = playback.canSkipPrevious,
                                canSkipNext = playback.canSkipNext,
                                textButtonColor = defaultTextButtonColor,
                                iconButtonColor = defaultIconButtonColor,
                                textBackgroundColor = defaultTextBackgroundColor,
                                icBackgroundColor = defaultIcBackgroundColor,
                                sliderPosition = null,
                                position = PreviewPositionMs,
                                duration = PreviewDurationMs,
                                playerConnection = connection,
                                navController = navController,
                                state = previewSheetState,
                                menuState = menuState,
                                bottomSheetPageState = bottomSheetPageState,
                                context = context,
                                onSliderValueChange = {},
                                onSliderValueChangeFinished = {},
                            )
                            Spacer(Modifier.height(20.dp))
                        }
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
