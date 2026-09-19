/*
 * LunarTune (2026)
 * © cognitiveshadows03 — github.com/cognitiveshadows03
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.citali.lunartune.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import dev.citali.lunartune.db.MusicDatabase
import dev.citali.lunartune.db.entities.Artist
import dev.citali.lunartune.db.entities.Song
import moe.rukamori.archivetune.innertube.YouTube
import moe.rukamori.archivetune.innertube.models.AlbumItem
import moe.rukamori.archivetune.innertube.models.ArtistItem
import moe.rukamori.archivetune.innertube.models.SongItem
import moe.rukamori.archivetune.innertube.models.WatchEndpoint
import moe.rukamori.archivetune.innertube.pages.ChartsPage
import moe.rukamori.archivetune.innertube.pages.MoodAndGenres
import javax.inject.Inject
import javax.inject.Singleton

data class SearchDiscoveryData(
    val moodAndGenres: List<MoodAndGenres.Item>,
    val newReleaseAlbums: List<AlbumItem>,
    val chartSections: List<ChartsPage.ChartSection>,
    val suggestedSongs: List<SongItem>,
    val searchedAlbums: List<AlbumItem>,
    val suggestedArtists: List<ArtistItem>,
)

@Singleton
class SearchDiscoveryRepository
    @Inject
    constructor(
        private val database: MusicDatabase,
    ) {
        private class CachedSnapshot(
            val data: SearchDiscoveryData,
            val expiresAtMs: Long,
        )

        // The screen's ViewModel is destination-scoped, so without this every visit to the
        // Search tab re-ran the whole fan-out (explore + charts + one search and up to
        // 2 × MaxSuggestionSeedItems related-content requests). Snapshot rules:
        //  - fresh (< CACHE_TTL_MS): returned as is;
        //  - expired but within STALE_GRACE_MS: returned immediately while a background
        //    refresh replaces it for the next visit;
        //  - the mutex keeps concurrent callers (two quick visits, or a visit during the
        //    background refresh) on ONE network load instead of duplicating it.
        @Volatile
        private var snapshot: CachedSnapshot? = null
        private val loadMutex = Mutex()
        private val refreshScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        suspend fun loadDiscovery(forceRefresh: Boolean = false): Result<SearchDiscoveryData> =
            withContext(Dispatchers.IO) {
                if (!forceRefresh) {
                    val now = System.currentTimeMillis()
                    snapshot?.let { cached ->
                        if (cached.expiresAtMs > now) {
                            return@withContext Result.success(cached.data)
                        }
                        if (cached.expiresAtMs > now - STALE_GRACE_MS) {
                            refreshScope.launch { revalidate() }
                            return@withContext Result.success(cached.data)
                        }
                    }
                }
                loadLocked(reuseFresh = !forceRefresh)
            }

        /** Background refresh of a stale snapshot; a no-op if another caller already refreshed it. */
        private suspend fun revalidate() {
            runCatching { loadLocked(reuseFresh = true) }
        }

        private suspend fun loadLocked(reuseFresh: Boolean): Result<SearchDiscoveryData> =
            loadMutex.withLock {
                if (reuseFresh) {
                    // Re-check after waiting: whoever held the lock may have just filled it.
                    snapshot?.let { cached ->
                        if (cached.expiresAtMs > System.currentTimeMillis()) {
                            return@withLock Result.success(cached.data)
                        }
                    }
                }
                loadDiscoveryFromNetwork().fold(
                    onSuccess = { data ->
                        snapshot =
                            CachedSnapshot(
                                data = data,
                                expiresAtMs = System.currentTimeMillis() + CACHE_TTL_MS,
                            )
                        Result.success(data)
                    },
                    onFailure = { throwable ->
                        // Serve the stale snapshot rather than an error state when the refresh fails.
                        snapshot
                            ?.takeIf { cached -> cached.expiresAtMs > System.currentTimeMillis() - STALE_GRACE_MS }
                            ?.let { cached -> Result.success(cached.data) }
                            ?: Result.failure(throwable)
                    },
                )
            }

        private suspend fun loadDiscoveryFromNetwork(): Result<SearchDiscoveryData> =
            withContext(Dispatchers.IO) {
                try {
                    coroutineScope {
                        val explorePageDeferred = async { YouTube.explore().getOrThrow() }
                        val chartsPageDeferred = async { YouTube.getChartsPage().getOrThrow() }
                        val suggestedSongsDeferred = async { loadSuggestedSongs() }
                        val searchedAlbumsDeferred =
                            async {
                                searchItems<AlbumItem>(
                                    query = TopAlbumsQuery,
                                    filter = YouTube.SearchFilter.FILTER_ALBUM,
                                )
                            }
                        val suggestedArtistsDeferred = async { loadSuggestedArtists() }

                        val explorePage = explorePageDeferred.await()
                        val chartsPage = chartsPageDeferred.await()

                        Result.success(
                            SearchDiscoveryData(
                                moodAndGenres = explorePage.moodAndGenres,
                                newReleaseAlbums = explorePage.newReleaseAlbums,
                                chartSections = chartsPage.sections,
                                suggestedSongs = suggestedSongsDeferred.await(),
                                searchedAlbums = searchedAlbumsDeferred.await(),
                                suggestedArtists = suggestedArtistsDeferred.await(),
                            ),
                        )
                    }
                } catch (throwable: Throwable) {
                    if (throwable is CancellationException) throw throwable
                    Result.failure(throwable)
                }
            }

        private suspend inline fun <reified T> searchItems(
            query: String,
            filter: YouTube.SearchFilter,
        ): List<T> =
            try {
                YouTube
                    .search(
                        query = query,
                        filter = filter,
                        useAccountContext = false,
                    ).getOrThrow()
                    .items
                    .filterIsInstance<T>()
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) throw throwable
                emptyList()
            }

        private suspend fun loadSuggestedSongs(): List<SongItem> =
            coroutineScope {
                val seedSongs =
                    database
                        .mostPlayedSongs(
                            fromTimeStamp = AllHistoryTimestamp,
                            limit = MaxHistoryLookupItems,
                        ).first()
                        .filterNot { song -> song.song.isLocal }
                        .take(MaxSuggestionSeedItems)
                val seedSongIds = seedSongs.mapTo(HashSet()) { song -> song.id }

                seedSongs
                    .map { song ->
                        async {
                            loadRelatedSongs(song)
                                .ifEmpty { searchRelatedSongs(song) }
                        }
                    }.awaitAll()
                    .flatten()
                    .filterNot { song -> song.id in seedSongIds }
                    .distinctBy { song -> song.id }
                    .take(MaxSuggestedItems)
            }

        private suspend fun loadRelatedSongs(song: Song): List<SongItem> =
            try {
                val nextResult = YouTube.next(WatchEndpoint(videoId = song.id)).getOrThrow()
                val relatedSongs =
                    nextResult
                        .relatedEndpoint
                        ?.let { endpoint -> YouTube.related(endpoint).getOrNull()?.songs }
                        .orEmpty()
                (relatedSongs + nextResult.items).distinctBy { item -> item.id }
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) throw throwable
                emptyList()
            }

        private suspend fun searchRelatedSongs(song: Song): List<SongItem> =
            searchItems(
                query =
                    buildString {
                        append(song.title)
                        song.artists
                            .firstOrNull()
                            ?.name
                            ?.takeIf(String::isNotBlank)
                            ?.let { artistName ->
                                append(' ')
                                append(artistName)
                            }
                    },
                filter = YouTube.SearchFilter.FILTER_SONG,
            )

        private suspend fun loadSuggestedArtists(): List<ArtistItem> =
            coroutineScope {
                val seedArtists =
                    database
                        .mostPlayedArtists(
                            fromTimeStamp = AllHistoryTimestamp,
                            limit = MaxHistoryLookupItems,
                        ).first()
                        .filter { artist -> artist.artist.isYouTubeArtist }
                        .take(MaxSuggestionSeedItems)
                val seedArtistIds = seedArtists.mapTo(HashSet()) { artist -> artist.id }

                seedArtists
                    .map { artist ->
                        async {
                            loadRelatedArtists(artist)
                                .ifEmpty { searchRelatedArtists(artist) }
                        }
                    }.awaitAll()
                    .flatten()
                    .filterNot { artist -> artist.id in seedArtistIds }
                    .distinctBy { artist -> artist.id }
                    .take(MaxSuggestedItems)
            }

        private suspend fun loadRelatedArtists(artist: Artist): List<ArtistItem> =
            try {
                YouTube
                    .artist(artist.id)
                    .getOrThrow()
                    .sections
                    .flatMap { section -> section.items }
                    .filterIsInstance<ArtistItem>()
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) throw throwable
                emptyList()
            }

        private suspend fun searchRelatedArtists(artist: Artist): List<ArtistItem> =
            searchItems(
                query = artist.title,
                filter = YouTube.SearchFilter.FILTER_ARTIST,
            )

        private companion object {
            const val CACHE_TTL_MS = 5L * 60 * 1000
            const val STALE_GRACE_MS = 30L * 60 * 1000
            const val AllHistoryTimestamp = 0L
            const val MaxHistoryLookupItems = 36
            const val MaxSuggestionSeedItems = 6
            const val MaxSuggestedItems = 12
            const val TopAlbumsQuery = "top albums"
        }
    }
