/*
 * LunarTune (2026)
 * © cognitiveshadows03 — github.com/cognitiveshadows03
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.citali.lunartune.ui.experimental

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Size
import dev.citali.lunartune.R
import dev.citali.lunartune.db.entities.Song
import dev.citali.lunartune.extensions.toMediaItem
import dev.citali.lunartune.extensions.togglePlayPause
import dev.citali.lunartune.models.MediaMetadata
import dev.citali.lunartune.models.toMediaMetadata
import dev.citali.lunartune.playback.PlayerConnection
import dev.citali.lunartune.playback.queues.ListQueue
import dev.citali.lunartune.playback.queues.YouTubeQueue
import dev.citali.lunartune.ui.component.MenuState
import dev.citali.lunartune.ui.menu.SongMenu
import dev.citali.lunartune.ui.utils.displayArtworkUrl
import kotlin.math.abs
import kotlin.math.min

private const val ExperimentalChartSize = 10
private const val ChartArtworkPx = 320

/**
 * "Charts deck": quick picks reimagined as a Top-10 chart pager. Giant rank
 * numerals, artwork cards and a springy page transform. Playback and menu
 * behaviour matches the classic list mode exactly.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExperimentalQuickPicksSection(
    quickPicks: List<Song>,
    mediaMetadata: MediaMetadata?,
    isPlaying: Boolean,
    navController: NavController,
    playerConnection: PlayerConnection,
    menuState: MenuState,
    haptic: HapticFeedback,
    modifier: Modifier = Modifier,
) {
    val songs = remember(quickPicks) { quickPicks.distinctBy { it.id }.take(ExperimentalChartSize) }
    if (songs.isEmpty()) return
    val pagerState = rememberPagerState(pageCount = { songs.size })

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            pageSpacing = 12.dp,
            contentPadding = PaddingValues(horizontal = 20.dp),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(200.dp),
        ) { page ->
            val song = songs[page]
            val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
            val damp = min(1f, abs(pageOffset))
            ChartCard(
                song = song,
                rank = page + 1,
                isActive = song.id == mediaMetadata?.id,
                isPlaying = isPlaying,
                onPlay = {
                    if (song.id == mediaMetadata?.id) {
                        playerConnection.player.togglePlayPause()
                    } else {
                        playerConnection.playQueue(
                            if (song.song.isLocal) {
                                ListQueue(items = listOf(song.toMediaItem()))
                            } else {
                                YouTubeQueue.radio(song.toMediaMetadata())
                            },
                        )
                    }
                },
                onMenu = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    menuState.show {
                        SongMenu(
                            originalSong = song,
                            navController = navController,
                            onDismiss = menuState::dismiss,
                        )
                    }
                },
                modifier =
                    Modifier.graphicsLayer {
                        val scale = 1f - 0.08f * damp
                        scaleX = scale
                        scaleY = scale
                        alpha = 1f - 0.35f * damp
                    },
            )
        }

        Spacer(Modifier.height(10.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
        ) {
            Text(
                text = "${pagerState.currentPage + 1} / ${songs.size}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(10.dp))
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth((pagerState.currentPage + 1) / songs.size.toFloat())
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChartCard(
    song: Song,
    rank: Int,
    isActive: Boolean,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val artworkUrl =
        remember(song.song.thumbnailUrl) {
            song.song.thumbnailUrl?.displayArtworkUrl(
                width = ChartArtworkPx,
                height = ChartArtworkPx,
            )
        }
    val imageRequest =
        remember(artworkUrl) {
            ImageRequest
                .Builder(context)
                .data(artworkUrl)
                .size(Size(ChartArtworkPx, ChartArtworkPx))
                .crossfade(true)
                .build()
        }

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp,
        border =
            if (isActive) {
                androidx.compose.foundation.BorderStroke(
                    2.dp,
                    MaterialTheme.colorScheme.primary,
                )
            } else {
                null
            },
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(
            modifier =
                Modifier.combinedClickable(
                    onClick = onPlay,
                    onLongClick = onMenu,
                ),
        ) {
            Text(
                text = rank.toString().padStart(2, '0'),
                fontSize = 108.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 10.dp),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
            ) {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier =
                        Modifier
                            .size(104.dp)
                            .clip(MaterialTheme.shapes.large),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = song.artists.joinToString { it.name },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Surface(
                            onClick = onPlay,
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(44.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter =
                                        painterResource(
                                            if (isActive && isPlaying) R.drawable.pause else R.drawable.play,
                                        ),
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }
                        Surface(
                            onClick = onMenu,
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(44.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(R.drawable.more_vert),
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
