/*
 * LunarTune (2026)
 * © cognitiveshadows03 — github.com/cognitiveshadows03
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.citali.lunartune.ui.experimental

import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import dev.citali.lunartune.R
import dev.citali.lunartune.db.entities.Album
import dev.citali.lunartune.db.entities.Artist
import dev.citali.lunartune.db.entities.LocalItem
import dev.citali.lunartune.db.entities.Playlist
import dev.citali.lunartune.db.entities.Song
import dev.citali.lunartune.extensions.toMediaItem
import dev.citali.lunartune.extensions.togglePlayPause
import dev.citali.lunartune.models.MediaMetadata
import dev.citali.lunartune.playback.PlayerConnection
import dev.citali.lunartune.playback.queues.ListQueue
import dev.citali.lunartune.ui.component.MenuState
import dev.citali.lunartune.ui.menu.AlbumMenu
import dev.citali.lunartune.ui.menu.ArtistMenu
import dev.citali.lunartune.ui.menu.PlaylistMenu
import dev.citali.lunartune.ui.menu.SongMenu
import dev.citali.lunartune.ui.theme.LunarMotion
import dev.citali.lunartune.ui.utils.YtimgResizePolicy
import dev.citali.lunartune.ui.utils.resize
import kotlinx.coroutines.CoroutineScope
import kotlin.math.abs
import kotlin.math.min
import kotlin.random.Random

private const val CoverArtworkWidthPx = 640
private const val CoverArtworkHeightPx = 400
private const val CoverFlowTiltDegrees = 14f

private data class CoverTile(
    val key: String,
    val localItem: LocalItem?,
)

/**
 * "Cover flow deck": pinned items reimagined as a tilted cover-flow carousel
 * with perspective side cards and segmented progress. Playback, navigation and
 * menu behaviour matches the classic grid exactly.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExperimentalSpeedDialSection(
    speedDialItems: List<LocalItem>,
    mediaMetadata: MediaMetadata?,
    isPlaying: Boolean,
    navController: NavController,
    playerConnection: PlayerConnection,
    menuState: MenuState,
    haptic: HapticFeedback,
    scope: CoroutineScope,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    val distinctSpeedDial =
        remember(speedDialItems) {
            speedDialItems
                .distinctBy {
                    when (it) {
                        is Song -> "song_${it.id}"
                        is Album -> "album_${it.id}"
                        is Artist -> "artist_${it.id}"
                        is Playlist -> "playlist_${it.id}"
                    }
                }.take(24)
        }
    val speedDialSongs = remember(distinctSpeedDial) { distinctSpeedDial.filterIsInstance<Song>() }
    val speedDialSongIndexById =
        remember(speedDialSongs) {
            speedDialSongs.mapIndexed { index, song -> song.id to index }.toMap()
        }
    val tiles =
        remember(distinctSpeedDial) {
            buildList {
                distinctSpeedDial.forEach { localItem ->
                    val key =
                        when (localItem) {
                            is Song -> "song_${localItem.id}"
                            is Album -> "album_${localItem.id}"
                            is Artist -> "artist_${localItem.id}"
                            is Playlist -> "playlist_${localItem.id}"
                        }
                    add(CoverTile(key = key, localItem = localItem))
                }
                add(CoverTile(key = "random", localItem = null))
            }
        }
    if (tiles.isEmpty()) return
    val pagerState = rememberPagerState(pageCount = { tiles.size })

    fun playSpeedDialQueue(startIndex: Int) {
        if (speedDialSongs.isEmpty()) return
        playerConnection.playQueue(
            ListQueue(
                title = context.getString(R.string.speed_dial),
                items = speedDialSongs.map { it.toMediaItem() },
                startIndex = startIndex,
            ),
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            pageSpacing = 12.dp,
            contentPadding = PaddingValues(horizontal = 44.dp),
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            val tile = tiles[page]
            val localItem = tile.localItem
            val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
            val damp = min(1f, abs(pageOffset))
            val clamped = pageOffset.coerceIn(-1f, 1f)
            if (localItem == null) {
                SurpriseCard(
                    onClick = {
                        if (speedDialSongs.isNotEmpty()) {
                            playSpeedDialQueue(Random.nextInt(speedDialSongs.size))
                        }
                    },
                    modifier =
                        Modifier.graphicsLayer {
                            rotationY = -CoverFlowTiltDegrees * clamped
                            cameraDistance = 12f * density.density
                            val scale = 1f - 0.12f * damp
                            scaleX = scale
                            scaleY = scale
                            alpha = 1f - 0.4f * damp
                        },
                )
            } else {
                val isActive =
                    when (localItem) {
                        is Song -> localItem.id == mediaMetadata?.id
                        is Album -> localItem.id == mediaMetadata?.album?.id
                        is Artist -> false
                        is Playlist -> false
                    }
                val songIndex =
                    if (localItem is Song) speedDialSongIndexById[localItem.id] ?: 0 else 0
                CoverCard(
                    localItem = localItem,
                    isActive = isActive,
                    isPlaying = isPlaying,
                    onClick = {
                        when (localItem) {
                            is Song -> {
                                if (isActive) {
                                    playerConnection.player.togglePlayPause()
                                } else {
                                    playSpeedDialQueue(songIndex)
                                }
                            }

                            is Album -> navController.navigate("album/${localItem.id}")
                            is Artist -> navController.navigate("artist/${localItem.id}")
                            is Playlist -> navController.navigate("local_playlist/${localItem.id}")
                        }
                    },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuState.show {
                            when (localItem) {
                                is Song -> {
                                    SongMenu(
                                        originalSong = localItem,
                                        navController = navController,
                                        onDismiss = menuState::dismiss,
                                    )
                                }

                                is Album -> {
                                    AlbumMenu(
                                        originalAlbum = localItem,
                                        navController = navController,
                                        onDismiss = menuState::dismiss,
                                    )
                                }

                                is Artist -> {
                                    ArtistMenu(
                                        originalArtist = localItem,
                                        coroutineScope = scope,
                                        onDismiss = menuState::dismiss,
                                    )
                                }

                                is Playlist -> {
                                    PlaylistMenu(
                                        playlist = localItem,
                                        coroutineScope = scope,
                                        onDismiss = menuState::dismiss,
                                    )
                                }
                            }
                        }
                    },
                    modifier =
                        Modifier.graphicsLayer {
                            rotationY = -CoverFlowTiltDegrees * clamped
                            cameraDistance = 12f * density.density
                            val scale = 1f - 0.12f * damp
                            scaleX = scale
                            scaleY = scale
                            alpha = 1f - 0.4f * damp
                        },
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        val maxVisibleSegments = 7
        if (tiles.size > maxVisibleSegments) {
            val dialStripState = rememberLazyListState()
            LaunchedEffect(pagerState.currentPage, tiles.size) {
                dialStripState.animateScrollToItem(
                    (pagerState.currentPage - 2).coerceIn(0, tiles.size - 1),
                )
            }
            LazyRow(
                state = dialStripState,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                contentPadding = PaddingValues(horizontal = 44.dp),
                userScrollEnabled = false,
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(count = tiles.size) { index ->
                    val active = index == pagerState.currentPage
                    val stripWidth by animateDpAsState(
                        targetValue = if (active) 22.dp else 6.dp,
                        animationSpec = LunarMotion.smooth(),
                        label = "dialStripWidth",
                    )
                    Box(
                        modifier =
                            Modifier
                                .width(stripWidth)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(
                                    if (active) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerHighest
                                    },
                                ),
                    )
                }
            }
        } else {
            val windowStart =
                (pagerState.currentPage - maxVisibleSegments / 2)
                    .coerceIn(0, (tiles.size - maxVisibleSegments).coerceAtLeast(0))
            val visibleCount = minOf(tiles.size, maxVisibleSegments)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 44.dp),
            ) {
                repeat(visibleCount) { i ->
                    val index = windowStart + i
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(
                                    if (index == pagerState.currentPage) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerHighest
                                    },
                                ),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CoverCard(
    localItem: LocalItem,
    isActive: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val artworkUrl =
        remember(localItem) {
            when (localItem) {
                is Song -> localItem.song.thumbnailUrl
                is Album -> localItem.album.thumbnailUrl
                is Artist -> localItem.artist.thumbnailUrl
                is Playlist -> localItem.thumbnails.firstOrNull()
            }?.resize(
                width = CoverArtworkWidthPx,
                height = CoverArtworkHeightPx,
                ytimgResizePolicy = YtimgResizePolicy.PreserveOriginal,
            )
        }
    val badge =
        when (localItem) {
            is Song -> "SONG"
            is Album -> "ALBUM"
            is Artist -> "ARTIST"
            is Playlist -> "PLAYLIST"
        }
    val subtitle =
        when (localItem) {
            is Song -> localItem.artists.joinToString { it.name }
            is Album -> localItem.artists.joinToString { it.name }
            is Artist -> "Artist"
            is Playlist -> "${localItem.songCount} songs"
        }
    val labelGradient =
        Brush.verticalGradient(
            colors =
                listOf(
                    Color.Transparent,
                    Color.Black.copy(alpha = 0.35f),
                    Color.Black.copy(alpha = 0.82f),
                ),
        )

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp,
        border =
            if (isActive) {
                BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            } else {
                null
            },
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(
            modifier =
                Modifier.combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                ),
        ) {
            AsyncImage(
                model =
                    remember(artworkUrl) {
                        ImageRequest
                            .Builder(context)
                            .data(artworkUrl)
                            // Software bitmaps: hardware bitmaps + rotationY tilt
                            // glitch into static on some old Adreno GPUs.
                            .allowHardware(false)
                            .build()
                    },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 10f),
            )
            Box(
                modifier =
                    Modifier
                        .matchParentSize()
                        .background(labelGradient),
            )
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.55f),
                contentColor = Color.White,
                modifier =
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
            ) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
            Icon(
                painter = painterResource(R.drawable.bookmark_filled),
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.92f),
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(22.dp),
            )
            Column(
                modifier =
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(14.dp),
            ) {
                Text(
                    text = localItem.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text =
                        if (isActive && isPlaying) {
                            "Now playing • $subtitle"
                        } else {
                            subtitle
                        },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun SurpriseCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gradient =
        Brush.linearGradient(
            colors =
                listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.tertiary,
                ),
        )
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .combinedClickable(onClick = onClick)
                    .background(gradient)
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                Spacer(Modifier.weight(1f))
                Icon(
                    painter = painterResource(R.drawable.playlist_play),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(44.dp),
                )
                Text(
                    text = "Surprise me",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Text(
                    text = "Shuffle your speed dial",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                )
                Spacer(Modifier.weight(1f))
            }
        }
    }
}
