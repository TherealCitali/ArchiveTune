/*
 * LunarTune (2026)
 * © cognitiveshadows03 — github.com/cognitiveshadows03
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package dev.citali.lunartune.ui.player

import android.content.res.Configuration
import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.compose.animation.core.tween
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.C
import androidx.media3.common.Player.STATE_BUFFERING
import androidx.media3.common.Player.STATE_READY
import androidx.navigation.NavController
import androidx.palette.graphics.Palette
import android.os.Build
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.size.Size
import coil3.toBitmap
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import dev.citali.lunartune.LocalDatabase
import dev.citali.lunartune.LocalPlayerConnection
import dev.citali.lunartune.R
import dev.citali.lunartune.constants.BlurRadiusKey
import dev.citali.lunartune.constants.DisableBlurKey
import dev.citali.lunartune.constants.EnableHapticFeedbackKey
import dev.citali.lunartune.constants.LyricsBackgroundStyle
import dev.citali.lunartune.constants.LyricsBackgroundStyleKey
import dev.citali.lunartune.constants.UseGpuBlurKey
import dev.citali.lunartune.constants.LyricsMode
import dev.citali.lunartune.constants.LyricsModeKey
import dev.citali.lunartune.constants.PlayerBackgroundStyle
import dev.citali.lunartune.constants.PlayerBackgroundStyleKey
import dev.citali.lunartune.constants.PlayerCustomBlurKey
import dev.citali.lunartune.constants.PlayerCustomBrightnessKey
import dev.citali.lunartune.constants.PlayerCustomContrastKey
import dev.citali.lunartune.constants.PlayerCustomImageUriKey
import dev.citali.lunartune.constants.ShowLyricsPlayerControlsKey
import dev.citali.lunartune.extensions.togglePlayPause
import dev.citali.lunartune.models.MediaMetadata
import dev.citali.lunartune.ui.component.LocalMenuState
import dev.citali.lunartune.ui.component.LyricsEnhanced
import dev.citali.lunartune.ui.component.LyricsV2
import dev.citali.lunartune.ui.component.PlayerSliderTrack
import dev.citali.lunartune.ui.menu.LyricsMenu
import dev.citali.lunartune.ui.theme.PlayerColorExtractor
import dev.citali.lunartune.utils.LyricsArtBlurCache
import dev.citali.lunartune.utils.makeTimeString
import dev.citali.lunartune.utils.rememberEnumPreference
import dev.citali.lunartune.utils.rememberPreference
import kotlin.coroutines.cancellation.CancellationException

private val AppleMusicFallbackGradient =
    listOf(
        Color(0xFF202020),
        Color(0xFF141414),
        Color(0xFF050505),
    )

@Suppress("UNUSED_PARAMETER")
@Composable
fun LyricsScreen(
    mediaMetadata: MediaMetadata,
    onBackClick: () -> Unit,
    navController: NavController,
    lyricsSyncOffset: Int,
    onLyricsSyncOffsetChange: (Int) -> Unit,
    onQueueClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    backHandlerEnabled: Boolean = true,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val player = playerConnection.player
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val view = LocalView.current

    val playbackState by playerConnection.playbackState.collectAsStateWithLifecycle()
    val isPlaying by playerConnection.isPlaying.collectAsStateWithLifecycle()
    val deviceMusicVolumeController = rememberDeviceMusicVolumeController()
    val onVolumeChange =
        remember(deviceMusicVolumeController) {
            { volume: Float ->
                deviceMusicVolumeController.setVolumeFraction(volume)
            }
        }
    val currentLyrics by playerConnection.currentLyrics.collectAsStateWithLifecycle(initialValue = null)

    val (enableHapticFeedback) = rememberPreference(EnableHapticFeedbackKey, true)
    val lyricsMode by rememberEnumPreference(LyricsModeKey, LyricsMode.ENHANCED)
    val playerBackground by rememberEnumPreference(PlayerBackgroundStyleKey, PlayerBackgroundStyle.DEFAULT)
    val configuredLyricsBackground by rememberEnumPreference(LyricsBackgroundStyleKey, LyricsBackgroundStyle.DEFAULT)
    val lyricsBackground = configuredLyricsBackground.resolveFor(playerBackground)
    val disableBlur by rememberPreference(DisableBlurKey, false)
    val useGpuBlur by rememberPreference(UseGpuBlurKey, true)
    val blurRadius by rememberPreference(BlurRadiusKey, 48f)
    val playerCustomImageUri by rememberPreference(PlayerCustomImageUriKey, "")
    val playerCustomBlur by rememberPreference(PlayerCustomBlurKey, 0f)
    val playerCustomContrast by rememberPreference(PlayerCustomContrastKey, 1f)
    val playerCustomBrightness by rememberPreference(PlayerCustomBrightnessKey, 1f)
    val foregroundColor =
        if (lyricsBackground == LyricsBackgroundStyle.FOLLOW_THEME) {
            MaterialTheme.colorScheme.onSurface
        } else {
            Color.White
        }
    val showPlayerControlsState =
        rememberPreference(ShowLyricsPlayerControlsKey, true)
    val showPlayerControls by showPlayerControlsState
    val onShowPlayerControlsChange =
        remember(showPlayerControlsState) {
            { showControls: Boolean ->
                showPlayerControlsState.value = showControls
            }
        }

    val hapticClick =
        remember(enableHapticFeedback, view) {
            {
                if (enableHapticFeedback) {
                    view.performHapticFeedback(
                        HapticFeedbackConstants.CONTEXT_CLICK,
                        HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING,
                    )
                }
            }
        }
    val lyricsHelper =
        remember(context) {
            EntryPointAccessors
                .fromApplication(
                    context.applicationContext,
                    dev.citali.lunartune.di.LyricsHelperEntryPoint::class.java,
                ).lyricsHelper()
        }

    LaunchedEffect(mediaMetadata.id, currentLyrics?.lyrics) {
        if (currentLyrics != null) return@LaunchedEffect
        try {
            val existingLyrics =
                withContext(Dispatchers.IO) {
                    database.lyrics(mediaMetadata.id).first()
                }
            if (existingLyrics != null) return@LaunchedEffect

            val lyrics =
                withContext(Dispatchers.IO) {
                    lyricsHelper.getLyrics(mediaMetadata)
                }
            withContext(Dispatchers.IO) {
                database.query {
                    insertLyricsIfAbsent(
                        id = mediaMetadata.id,
                        lyrics = lyrics,
                    )
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
    }

    val positionState = remember(mediaMetadata.id) { mutableLongStateOf(0L) }
    val durationState = remember(mediaMetadata.id) { mutableLongStateOf(C.TIME_UNSET) }
    var sliderPosition by remember(mediaMetadata.id) { mutableStateOf<Long?>(null) }
    var gradientColors by remember(mediaMetadata.thumbnailUrl) { mutableStateOf(AppleMusicFallbackGradient) }

    val gradientColorsCache =
        remember {
            object : LinkedHashMap<String, List<Color>>(20, 0.75f, true) {
                override fun removeEldestEntry(eldest: Map.Entry<String, List<Color>>) = size > 20
            }
        }
    val fallbackColor = remember { Color.Black.toArgb() }

    LaunchedEffect(mediaMetadata.id, mediaMetadata.thumbnailUrl, lyricsBackground) {
        if (lyricsBackground != LyricsBackgroundStyle.DEFAULT && lyricsBackground != LyricsBackgroundStyle.COLORING) {
            gradientColors = AppleMusicFallbackGradient
            return@LaunchedEffect
        }
        val thumbnailUrl = mediaMetadata.thumbnailUrl
        if (thumbnailUrl == null) {
            gradientColors = AppleMusicFallbackGradient
            return@LaunchedEffect
        }

        gradientColorsCache[thumbnailUrl]?.let {
            gradientColors = it
            return@LaunchedEffect
        }

        gradientColors = AppleMusicFallbackGradient

        val request =
            ImageRequest
                .Builder(context)
                .data(thumbnailUrl)
                .size(Size(PlayerColorExtractor.Config.IMAGE_SIZE, PlayerColorExtractor.Config.IMAGE_SIZE))
                .allowHardware(false)
                .build()

        val extractedColors =
            try {
                val image =
                    withContext(Dispatchers.IO) {
                        context.imageLoader.execute(request)
                    }.image
                if (image == null) {
                    null
                } else {
                    val bitmap = image.toBitmap()
                    withContext(Dispatchers.Default) {
                        val palette =
                            Palette
                                .from(bitmap)
                                .maximumColorCount(PlayerColorExtractor.Config.MAX_COLOR_COUNT)
                                .resizeBitmapArea(PlayerColorExtractor.Config.BITMAP_AREA)
                                .generate()
                        PlayerColorExtractor.extractGradientColors(
                            palette = palette,
                            fallbackColor = fallbackColor,
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }

        gradientColors = extractedColors ?: AppleMusicFallbackGradient
        gradientColorsCache[thumbnailUrl] = gradientColors
    }

    LaunchedEffect(player, playbackState, mediaMetadata.id) {
        if (playbackState != STATE_READY && playbackState != STATE_BUFFERING) return@LaunchedEffect
        while (isActive) {
            positionState.longValue = player.currentPosition.coerceAtLeast(0L)
            durationState.longValue = player.duration
            delay(250)
        }
    }

    val showLyricsMenu = {
        menuState.show {
            LyricsMenu(
                lyricsProvider = { currentLyrics },
                mediaMetadataProvider = { mediaMetadata },
                lyricsSyncOffset = lyricsSyncOffset,
                onLyricsSyncOffsetChange = onLyricsSyncOffsetChange,
                showPlayerControlsState = showPlayerControlsState,
                onShowPlayerControlsChange = onShowPlayerControlsChange,
                onDismiss = menuState::dismiss,
            )
        }
    }

    val isLoading = playbackState == STATE_BUFFERING || sliderPosition != null
    val orientation = LocalConfiguration.current.orientation

    BackHandler(enabled = backHandlerEnabled, onBack = onBackClick)

    Box(
        modifier =
            modifier
                .fillMaxSize(),
    ) {
        LyricsScreenBackground(
            style = lyricsBackground,
            mediaMetadata = mediaMetadata,
            gradientColors = gradientColors,
            disableBlur = disableBlur,
            useGpuBlur = useGpuBlur,
            blurRadius = blurRadius,
            playerCustomImageUri = playerCustomImageUri,
            playerCustomBlur = playerCustomBlur,
            playerCustomContrast = playerCustomContrast,
            playerCustomBrightness = playerCustomBrightness,
        )

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .consumeUnhandledPointerInput(),
        )

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars),
        ) {
            AppleMusicGrabber(onClick = onBackClick)
            AppleMusicTrackHeader(
                mediaMetadata = mediaMetadata,
                foregroundColor = foregroundColor,
                onMoreClick = showLyricsMenu,
                onDismissClick = onBackClick,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
            )

            if (orientation == Configuration.ORIENTATION_LANDSCAPE && showPlayerControls) {
                Row(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 36.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppleMusicLyricsPane(
                        lyricsMode = lyricsMode,
                        foregroundColor = foregroundColor,
                        sliderPositionProvider = { sliderPosition },
                        lyricsSyncOffset = lyricsSyncOffset,
                        modifier =
                            Modifier
                                .weight(1.15f)
                                .fillMaxHeight()
                                .padding(end = 32.dp),
                    )

                    Column(
                Modifier
                                .weight(0.85f)
                                .widthIn(max = 420.dp),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        AppleMusicControls(
                            positionProvider = { positionState.longValue },
                            durationProvider = { durationState.longValue },
                            sliderPosition = sliderPosition,
                            isPlaying = isPlaying,
                            isLoading = isLoading,
                            volume = deviceMusicVolumeController.volumeFraction,
                            onPositionChange = { sliderPosition = it },
                            onPositionChangeFinished = {
                                sliderPosition?.let {
                                    player.seekTo(it)
                                    positionState.longValue = it
                                }
                                sliderPosition = null
                            },
                            onVolumeChange = onVolumeChange,
                            onPreviousClick = {
                                hapticClick()
                                playerConnection.seekToPrevious()
                            },
                            onPlayPauseClick = {
                                hapticClick()
                                player.togglePlayPause()
                            },
                            onNextClick = {
                                hapticClick()
                                playerConnection.seekToNext()
                            },
                            foregroundColor = foregroundColor,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            } else {
                AppleMusicLyricsPane(
                    lyricsMode = lyricsMode,
                    foregroundColor = foregroundColor,
                    sliderPositionProvider = { sliderPosition },
                    lyricsSyncOffset = lyricsSyncOffset,
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                )

                if (showPlayerControls) {
                    AppleMusicControls(
                        positionProvider = { positionState.longValue },
                        durationProvider = { durationState.longValue },
                        sliderPosition = sliderPosition,
                        isPlaying = isPlaying,
                        isLoading = isLoading,
                        volume = deviceMusicVolumeController.volumeFraction,
                        onPositionChange = { sliderPosition = it },
                        onPositionChangeFinished = {
                            sliderPosition?.let {
                                player.seekTo(it)
                                positionState.longValue = it
                            }
                            sliderPosition = null
                        },
                        onVolumeChange = onVolumeChange,
                        onPreviousClick = {
                            hapticClick()
                            playerConnection.seekToPrevious()
                        },
                        onPlayPauseClick = {
                            hapticClick()
                            player.togglePlayPause()
                        },
                        onNextClick = {
                            hapticClick()
                            playerConnection.seekToNext()
                        },
                        foregroundColor = foregroundColor,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 40.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun LyricsScreenBackground(
    style: LyricsBackgroundStyle,
    mediaMetadata: MediaMetadata,
    gradientColors: List<Color>,
    disableBlur: Boolean,
    useGpuBlur: Boolean,
    blurRadius: Float,
    playerCustomImageUri: String,
    playerCustomBlur: Float,
    playerCustomContrast: Float,
    playerCustomBrightness: Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(
                    if (style == LyricsBackgroundStyle.FOLLOW_THEME) {
                        MaterialTheme.colorScheme.surface
                    } else {
                        Color.Black
                    },
                ),
    ) {
        when (style) {
            LyricsBackgroundStyle.DEFAULT -> {
                AppleMusicBackground(
                    mediaMetadata = mediaMetadata,
                    gradientColors = gradientColors,
                )
            }

            LyricsBackgroundStyle.FOLLOW_THEME -> Unit

            LyricsBackgroundStyle.MOVING_BLUR -> {
                MovingBlurBackground(
                    mediaMetadata = mediaMetadata,
                    gradientColors = gradientColors,
                    useGpuBlur = useGpuBlur,
                    blurRadius = blurRadius,
                    disableBlur = disableBlur,
                )
            }

            LyricsBackgroundStyle.COLORING,
            LyricsBackgroundStyle.CUSTOM,
            -> {
                PlayerBackground(
                    playerBackground =
                        if (style == LyricsBackgroundStyle.CUSTOM) {
                            PlayerBackgroundStyle.CUSTOM
                        } else {
                            PlayerBackgroundStyle.COLORING
                        },
                    mediaMetadata = mediaMetadata,
                    gradientColors = gradientColors,
                    disableBlur = disableBlur,
                    blurRadius = blurRadius,
                    playerCustomImageUri = playerCustomImageUri,
                    playerCustomBlur = playerCustomBlur,
                    playerCustomContrast = playerCustomContrast,
                    playerCustomBrightness = playerCustomBrightness,
                )
            }
        }
    }
}

@Composable
private fun AppleMusicBackground(
    mediaMetadata: MediaMetadata,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier,
) {
    val colors = if (gradientColors.isNotEmpty()) gradientColors else AppleMusicFallbackGradient
    val backgroundBrush =
        remember(colors) {
            Brush.verticalGradient(
                listOf(
                    // The palette has to tint the artwork, not replace it. At 0.88 / 0.76 / 0.96,
                    // with the artwork itself at 0.62, less than a tenth of the cover survived, so
                    // the backdrop read as a flat opaque colour instead of as a blurred cover.
                    // These leave about as much of the artwork visible as the old flat
                    // Black @ 0.52 scrim did — but the rest of the pixel is palette colour rather
                    // than black, which is where the vividness comes from.
                    colors.getOrElse(0) { AppleMusicFallbackGradient[0] }.copy(alpha = AppleMusicScrimTop),
                    colors.getOrElse(1) { AppleMusicFallbackGradient[1] }.copy(alpha = AppleMusicScrimMid),
                    colors.getOrElse(2) { AppleMusicFallbackGradient[2] }.copy(alpha = AppleMusicScrimBottom),
                ),
            )
        }
    val bottomScrim =
        remember {
            Brush.verticalGradient(
                listOf(
                    Color.Transparent,
                    Color.Black.copy(alpha = 0.28f),
                ),
            )
        }

    val context = LocalContext.current
    val thumbnailUrl = mediaMetadata.thumbnailUrl
    val cacheRevision by LyricsArtBlurCache.updates.collectAsState()
    val blurredArt =
        remember(thumbnailUrl, cacheRevision) {
            LyricsArtBlurCache.peek(thumbnailUrl)
        }

    LaunchedEffect(thumbnailUrl) {
        LyricsArtBlurCache.prefetch(context, thumbnailUrl)
    }

    // The blurred cover sits under the track's own palette rather than under a flat black scrim,
    // which is what makes this read as an Apple Music backdrop instead of a dimmed thumbnail.
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(AppleMusicFallbackGradient.last()),
    ) {
        // Keyed on the bitmap, not the url: the outgoing artwork stays on screen until the incoming
        // one is actually ready, so a track change never flashes an empty backdrop.
        Crossfade(
            targetState = blurredArt,
            animationSpec = tween(BACKDROP_FADE_MS),
            label = "appleMusicBackdrop",
            modifier =
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = AppleMusicBackdropScale
                        scaleY = AppleMusicBackdropScale
                    },
        ) { art ->
            if (art != null) {
                Image(
                    bitmap = art.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(backgroundBrush),
        )
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.10f)),
        )
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(bottomScrim),
        )
    }
}

private const val AppleMusicBackdropScale = 1.12f

/**
 * How much of the track's own palette is laid over the blurred artwork, top to bottom. Lower these
 * to let more of the cover through; raise them for a flatter, more coloured backdrop.
 */
private const val AppleMusicScrimTop = 0.55f
private const val AppleMusicScrimMid = 0.45f
private const val AppleMusicScrimBottom = 0.66f

/**
 * How long the backdrop takes to trade one track's artwork for the next. Without it the backdrop
 * pops the instant the new bitmap lands, which is the one thing that makes the whole effect read
 * as a glitch rather than as a finish.
 */
private const val BACKDROP_FADE_MS = 700

@Composable
private fun MovingBlurBackground(
    mediaMetadata: MediaMetadata,
    gradientColors: List<Color>,
    useGpuBlur: Boolean,
    blurRadius: Float,
    disableBlur: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = if (gradientColors.isNotEmpty()) gradientColors else AppleMusicFallbackGradient
    val backgroundBrush =
        remember(colors) {
            Brush.verticalGradient(
                listOf(
                    // Much lighter than the static AppleMusicBackground (0.88 / 0.76 / 0.96).
                    // Those alphas sit fine over that backdrop because it is hardly blurred, so
                    // the artwork still reads through them; under a 77dp blur the palette is
                    // nearly all you can see, and a dark palette made the page read as almost
                    // black. The bottom stays the heaviest of the three so lyrics keep their
                    // contrast where they are actually read.
                    colors.getOrElse(0) { AppleMusicFallbackGradient[0] }.copy(alpha = MOVING_BLUR_SCRIM_TOP),
                    colors.getOrElse(1) { AppleMusicFallbackGradient[1] }.copy(alpha = MOVING_BLUR_SCRIM_MID),
                    colors.getOrElse(2) { AppleMusicFallbackGradient[2] }.copy(alpha = MOVING_BLUR_SCRIM_BOTTOM),
                ),
            )
        }
    val bottomScrim =
        remember {
            Brush.verticalGradient(
                listOf(
                    Color.Transparent,
                    Color.Black.copy(alpha = 0.18f),
                ),
            )
        }

    // Saturation and a brightness lift, applied only to this backdrop — it does not touch the
    // shared PlayerColorExtractor palette that other screens consume. Heavy blurring averages a
    // cover towards its own mean, which is usually dark and flat, so the artwork needs both to
    // stay legible as colour under the scrim. ColorMatrix is built by hand because
    // androidx.compose.ui.graphics.ColorMatrix has no setSaturation(): the terms below are the
    // standard Rec. 709 saturation matrix (sat = 1 gives the identity), each row then scaled by
    // [MOVING_BLUR_BRIGHTNESS] to lift the result.
    val vibrancyColorFilter =
        remember {
            val s = MOVING_BLUR_SATURATION
            val gain = MOVING_BLUR_BRIGHTNESS
            // Rec. 709 saturation matrix. The rows are deliberately *not* identical: each output
            // channel keeps its own channel at (luma + s) and takes the other two at
            // (luma * (1 - s)). Writing one row three times — which is what the ported version of
            // this did — computes the same weighted sum for R, G and B, and that is a greyscale
            // conversion however high s goes: a blue sky came out grey, and turning the saturation
            // up only made the grey brighter. At s = 1 this is the identity matrix.
            val lr = 0.213f * (1f - s)
            val lg = 0.715f * (1f - s)
            val lb = 0.072f * (1f - s)
            ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        (lr + s) * gain, lg * gain, lb * gain, 0f, 0f,
                        lr * gain, (lg + s) * gain, lb * gain, 0f, 0f,
                        lr * gain, lg * gain, (lb + s) * gain, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f,
                    ),
                ),
            )
        }

    val context = LocalContext.current
    val thumbnailUrl = mediaMetadata.thumbnailUrl
    val cacheRevision by LyricsArtBlurCache.updates.collectAsState()
    val blurredArt =
        remember(thumbnailUrl, cacheRevision) {
            LyricsArtBlurCache.peek(thumbnailUrl)
        }

    LaunchedEffect(thumbnailUrl) {
        LyricsArtBlurCache.prefetch(context, thumbnailUrl)
    }

    // Modifier.blur is a no-op below Android 12 — it needs RenderEffect, API 31+ — so pre-S, and
    // Android 12+ with the toggle off, draw the bitmap LyricsArtBlurCache blurred once on the CPU
    // instead. The drift is a graphicsLayer transform either way, and a canvas transform is
    // something every API level can do, so the backdrop still moves on old devices. (Animating a
    // pre-blurred bitmap with a layout-phase Modifier.offset instead is what tears on them.)
    val gpuBlur = useGpuBlur && !disableBlur && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val wander = rememberBlurWanderDrift(active = thumbnailUrl != null)

    val gpuRequest =
        remember(context, thumbnailUrl) {
            thumbnailUrl?.let { url ->
                ImageRequest
                    .Builder(context)
                    .data(url)
                    // The blur destroys the detail anyway, and the layer is rasterised at the
                    // footprint size below, so a small decode keeps both the bitmap and the
                    // per-frame GPU blur cheap.
                    .size(MOVING_BLUR_ART_PX)
                    .build()
            }
        }

    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxSize()
                .clipToBounds()
                .background(AppleMusicFallbackGradient.last()),
    ) {
        // The blur clips to its own bounds, so the layer cannot simply be screen-shaped: rotated
        // by the walk, a screen-sized rectangle only covers its inscribed circle and a black wedge
        // sweeps through a corner. This sizes it to the container's furthest corner instead.
        val footprint =
            remember(maxWidth, maxHeight) {
                blurBackdropFootprint(
                    width = maxWidth,
                    height = maxHeight,
                    restScale = MOVING_BLUR_SCALE,
                    driftScale = MOVING_BLUR_SCALE,
                )
            }

        if (gpuBlur && gpuRequest != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                val driftModifier =
                    Modifier
                        .requiredSize(footprint)
                        .graphicsLayer {
                            scaleX = MOVING_BLUR_SCALE
                            scaleY = MOVING_BLUR_SCALE
                            // Deferred, draw-phase reads — see BlurWanderDrift.
                            translationX = wander.xDp.floatValue.dp.toPx()
                            translationY = wander.yDp.floatValue.dp.toPx()
                            // Rotation is the only part of the walk that can carry a colour across
                            // the whole surface; translation moves every colour by the same vector.
                            rotationZ = wander.rotationDeg.floatValue
                            compositingStrategy = CompositingStrategy.Offscreen
                        }
                        .alpha(MOVING_BLUR_ALPHA)

                // Blur only when there is something to blur: a zero radius is not a no-op at the
                // RenderEffect level, it is an invalid argument.
                val blurModifier =
                    if (blurRadius > 0.5f) {
                        Modifier.blur((blurRadius * MOVING_BLUR_BLUR_GAIN / MOVING_BLUR_SCALE).dp)
                    } else {
                        Modifier
                    }

                // The crossfade sits under the blur, so what fades is the artwork and not the
                // finished blurred result — no sharp edge is ever visible mid-transition.
                Crossfade(
                    targetState = gpuRequest,
                    animationSpec = tween(BACKDROP_FADE_MS),
                    label = "movingBlurBackdrop",
                    // blur INSIDE the transform: the artwork is blurred while it is still centred
                    // and only then moved, so the blur never samples the transparent area behind
                    // the layer's trailing edge.
                    modifier = driftModifier.then(blurModifier),
                ) { request ->
                    AsyncImage(
                        model = request,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        colorFilter = vibrancyColorFilter,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        } else if (blurredArt != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                // Keyed on the bitmap, so the old artwork holds the frame until the new one has
                // been blurred and cached — the CPU path is the one that would otherwise flash.
                Crossfade(
                    targetState = blurredArt,
                    animationSpec = tween(BACKDROP_FADE_MS),
                    label = "movingBlurBackdrop",
                    modifier =
                        Modifier
                            .requiredSize(footprint)
                            .graphicsLayer {
                                scaleX = MOVING_BLUR_SCALE
                                scaleY = MOVING_BLUR_SCALE
                                translationX = wander.xDp.floatValue.dp.toPx()
                                translationY = wander.yDp.floatValue.dp.toPx()
                                rotationZ = wander.rotationDeg.floatValue
                                compositingStrategy = CompositingStrategy.Offscreen
                            }
                            .alpha(MOVING_BLUR_ALPHA),
                ) { art ->
                    Image(
                        bitmap = art.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        colorFilter = vibrancyColorFilter,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(backgroundBrush),
        )

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(bottomScrim),
        )
    }
}

/**
 * How far the layer is scaled up. It has to be large enough that the walk's drift and rotation
 * can never pull the artwork's own edge into view — see [blurBackdropFootprint].
 */
private const val MOVING_BLUR_SCALE = 2.4f

/**
 * The layer is drawn scaled by [MOVING_BLUR_SCALE], and Compose scales the blur along with it, so
 * the on-screen radius is `radius * MOVING_BLUR_SCALE`. This gain turns the Blur intensity slider
 * (0..64, default 48) into the ~77dp of on-screen blur the backdrop wants at its default.
 */
private const val MOVING_BLUR_BLUR_GAIN = 1.6f

private const val MOVING_BLUR_ALPHA = 1f

/**
 * How much of the track's own palette is laid over the blurred artwork, top to bottom.
 *
 * Lower these to let more of the artwork through — this is the control to reach for if the
 * moving-blur page still reads as too dim on your covers.
 */
private const val MOVING_BLUR_SCRIM_TOP = 0.55f
private const val MOVING_BLUR_SCRIM_MID = 0.45f
private const val MOVING_BLUR_SCRIM_BOTTOM = 0.68f

private const val MOVING_BLUR_SATURATION = 1.6f

/**
 * Only a slight lift now. The larger gain this used to carry was compensating for the filter
 * turning everything grey — with real saturation, much more than this blows the highlights out.
 */
private const val MOVING_BLUR_BRIGHTNESS = 1.06f

/** Decode size for the drifting artwork — the blur hides everything finer than this. */
private const val MOVING_BLUR_ART_PX = 256

@Composable
private fun AppleMusicGrabber(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val closeDescription = stringResource(R.string.close)
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(44.dp)
                .semantics { contentDescription = closeDescription }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Button,
                    onClick = onClick,
                ),
    )
}

@Composable
private fun AppleMusicTrackHeader(
    mediaMetadata: MediaMetadata,
    foregroundColor: Color,
    onMoreClick: () -> Unit,
    onDismissClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val artistText =
        remember(mediaMetadata.id, mediaMetadata.artists) {
            mediaMetadata.artists.joinToString { it.name }
        }

    Row(
        modifier = modifier.heightIn(min = 64.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(foregroundColor.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = mediaMetadata.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (mediaMetadata.thumbnailUrl == null) {
                Icon(
                    painter = painterResource(R.drawable.music_note),
                    contentDescription = null,
                    tint = foregroundColor.copy(alpha = 0.72f),
                    modifier = Modifier.size(26.dp),
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = mediaMetadata.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = foregroundColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = artistText,
                style = MaterialTheme.typography.bodyLarge,
                color = foregroundColor.copy(alpha = 0.72f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        AppleMusicHeaderIconButton(
            iconRes = R.drawable.close,
            contentDescription = stringResource(R.string.close),
            foregroundColor = foregroundColor,
            onClick = onDismissClick,
        )

        Spacer(modifier = Modifier.width(4.dp))

        AppleMusicHeaderIconButton(
            iconRes = R.drawable.more_horiz,
            contentDescription = stringResource(R.string.more_options),
            foregroundColor = foregroundColor,
            onClick = onMoreClick,
        )
    }
}

@Composable
private fun AppleMusicHeaderIconButton(
    iconRes: Int,
    contentDescription: String,
    foregroundColor: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(48.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = false, radius = 24.dp),
                    role = Role.Button,
                    onClick = onClick,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(foregroundColor.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = contentDescription,
                tint = foregroundColor,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun AppleMusicLyricsPane(
    lyricsMode: LyricsMode,
    foregroundColor: Color,
    sliderPositionProvider: () -> Long?,
    lyricsSyncOffset: Int,
    modifier: Modifier = Modifier,
) {
    LyricsContent(
        lyricsMode = lyricsMode,
        sliderPositionProvider = sliderPositionProvider,
        lyricsSyncOffset = lyricsSyncOffset,
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
        textColor = foregroundColor,
    )
}

@Composable
private fun AppleMusicControls(
    positionProvider: () -> Long,
    durationProvider: () -> Long,
    sliderPosition: Long?,
    isPlaying: Boolean,
    isLoading: Boolean,
    volume: Float,
    onPositionChange: (Long) -> Unit,
    onPositionChangeFinished: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onPreviousClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    foregroundColor: Color,
    modifier: Modifier = Modifier,
) {
    val position = positionProvider()
    val duration = durationProvider()
    val hasDuration = duration != C.TIME_UNSET && duration > 0L
    val safeDuration = if (hasDuration) duration else 1L
    val currentPosition = (sliderPosition ?: position).coerceIn(0L, safeDuration)
    val remainingPosition = (safeDuration - currentPosition).coerceAtLeast(0L)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppleMusicSlider(
            value = currentPosition.toFloat(),
            valueRange = 0f..safeDuration.toFloat(),
            activeColor = foregroundColor.copy(alpha = 0.94f),
            inactiveColor = foregroundColor.copy(alpha = 0.28f),
            trackHeight = 8.dp,
            onValueChange = { onPositionChange(it.toLong()) },
            onValueChangeFinished = onPositionChangeFinished,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = makeTimeString(currentPosition),
                style = MaterialTheme.typography.labelMedium,
                color = foregroundColor.copy(alpha = 0.54f),
            )
            Text(
                text = if (hasDuration) "-${makeTimeString(remainingPosition)}" else "",
                style = MaterialTheme.typography.labelMedium,
                color = foregroundColor.copy(alpha = 0.54f),
            )
        }

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 26.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppleMusicTransportButton(
                iconRes = R.drawable.skip_previous,
                contentDescription = stringResource(R.string.widget_previous),
                iconSize = 44.dp,
                touchSize = 68.dp,
                foregroundColor = foregroundColor,
                onClick = onPreviousClick,
            )
            IconButton(
                onClick = onPlayPauseClick,
                modifier = Modifier.size(74.dp),
            ) {
                if (isLoading) {
                    CircularWavyProgressIndicator(
                        modifier = Modifier.size(42.dp),
                        color = foregroundColor,
                    )
                } else {
                    Icon(
                        painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                        contentDescription =
                            if (isPlaying) {
                                stringResource(R.string.widget_pause)
                            } else {
                                stringResource(R.string.play)
                            },
                        tint = foregroundColor,
                        modifier = Modifier.size(54.dp),
                    )
                }
            }
            AppleMusicTransportButton(
                iconRes = R.drawable.skip_next,
                contentDescription = stringResource(R.string.next),
                iconSize = 44.dp,
                touchSize = 68.dp,
                foregroundColor = foregroundColor,
                onClick = onNextClick,
            )
        }

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 26.dp, bottom = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.volume_off),
                contentDescription = stringResource(R.string.minimum_volume),
                tint = foregroundColor.copy(alpha = 0.66f),
                modifier = Modifier.size(17.dp),
            )
            AppleMusicSlider(
                value = volume.coerceIn(0f, 1f),
                valueRange = 0f..1f,
                activeColor = foregroundColor.copy(alpha = 0.88f),
                inactiveColor = foregroundColor.copy(alpha = 0.24f),
                trackHeight = 8.dp,
                onValueChange = onVolumeChange,
                onValueChangeFinished = {},
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
            )
            Icon(
                painter = painterResource(R.drawable.volume_up),
                contentDescription = stringResource(R.string.maximum_volume),
                tint = foregroundColor.copy(alpha = 0.66f),
                modifier = Modifier.size(19.dp),
            )
        }
    }
}

@Composable
private fun AppleMusicTransportButton(
    iconRes: Int,
    contentDescription: String?,
    iconSize: Dp,
    touchSize: Dp,
    foregroundColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(touchSize),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = foregroundColor,
            modifier = Modifier.size(iconSize),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppleMusicSlider(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    activeColor: Color,
    inactiveColor: Color,
    trackHeight: Dp,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val safeStart = valueRange.start
    val safeEnd = valueRange.endInclusive.coerceAtLeast(safeStart + 1f)
    val safeRange = safeStart..safeEnd
    val sliderColors =
        SliderDefaults.colors(
            activeTrackColor = activeColor,
            activeTickColor = activeColor,
            thumbColor = Color.Transparent,
            inactiveTrackColor = inactiveColor,
        )

    Slider(
        value = value.coerceIn(safeRange),
        valueRange = safeRange,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        colors = sliderColors,
        thumb = { Spacer(modifier = Modifier.size(0.dp)) },
        track = { sliderState ->
            PlayerSliderTrack(
                sliderState = sliderState,
                colors = sliderColors,
                trackHeight = trackHeight,
            )
        },
        modifier = modifier.height(28.dp),
    )
}

@Composable
private fun LyricsContent(
    lyricsMode: LyricsMode,
    sliderPositionProvider: () -> Long?,
    lyricsSyncOffset: Int,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    when (lyricsMode) {
        LyricsMode.V2 -> {
            LyricsV2(
                sliderPositionProvider = sliderPositionProvider,
                lyricsSyncOffset = lyricsSyncOffset,
                modifier = modifier,
                textColorOverride = textColor,
            )
        }

        LyricsMode.ENHANCED -> {
            LyricsEnhanced(
                sliderPositionProvider = sliderPositionProvider,
                lyricsSyncOffset = lyricsSyncOffset,
                modifier = modifier,
                textColorOverride = textColor,
            )
        }
    }
}
