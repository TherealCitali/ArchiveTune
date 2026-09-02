/*
 * LunarTune (2026)
 * © cognitiveshadows03 — github.com/cognitiveshadows03
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.citali.lunartune.home

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import dev.citali.lunartune.constants.DisableBlurKey
import dev.citali.lunartune.constants.QuickPicks
import dev.citali.lunartune.constants.QuickPicksKey
import dev.citali.lunartune.constants.QuickPicksDisplayMode
import dev.citali.lunartune.constants.QuickPicksDisplayModeKey
import dev.citali.lunartune.constants.ShowHomeCategoryChipsKey
import dev.citali.lunartune.db.MusicDatabase
import dev.citali.lunartune.db.entities.Song
import dev.citali.lunartune.extensions.toEnum
import dev.citali.lunartune.utils.dataStore
import moe.rukamori.archivetune.innertube.YouTube
import moe.rukamori.archivetune.innertube.models.SongItem
import moe.rukamori.archivetune.innertube.models.WatchEndpoint
import javax.inject.Inject

class HomeRepository
    @Inject
    constructor(
        @ApplicationContext context: Context,
        private val database: MusicDatabase,
    ) {
        val showCategoryChips: Flow<Boolean> =
            context.dataStore.data
                .map { preferences -> preferences[ShowHomeCategoryChipsKey] ?: true }
                .distinctUntilChanged()

        val quickPicksDisplayMode: Flow<QuickPicksDisplayMode> =
            context.dataStore.data
                .map { preferences ->
                    preferences[QuickPicksDisplayModeKey].toEnum(QuickPicksDisplayMode.CARD)
                }.distinctUntilChanged()

        val quickPicksMode: Flow<QuickPicks> =
            context.dataStore.data
                .map { preferences -> preferences[QuickPicksKey].toEnum(QuickPicks.QUICK_PICKS) }
                .distinctUntilChanged()

        val showTonalBackdrop: Flow<Boolean> =
            context.dataStore.data
                .map { preferences -> preferences[DisableBlurKey] != true }
                .distinctUntilChanged()

        /**
         * Songs from the listening history that YouTube Music can build recommendations
         * from. Most played songs of the last months come first, recent songs are used
         * when there is nothing played often enough yet.
         */
        suspend fun loadQuickPickSeeds(limit: Int): List<Song> {
            val fromTimestamp = System.currentTimeMillis() - QUICK_PICKS_HISTORY_WINDOW_MS
            val mostPlayed =
                database
                    .mostPlayedSongs(fromTimestamp, limit = limit * 3)
                    .first()
                    .filter { song -> song.isYouTubeRecommendationSeed() }
            val candidates =
                mostPlayed.ifEmpty {
                    database
                        .recentSongs(limit = limit * 6)
                        .first()
                        .filter { song -> song.isYouTubeRecommendationSeed() }
                }
            return candidates
                .distinctBy { song -> song.artists.firstOrNull()?.id ?: song.id }
                .take(limit)
        }

        /** "Up next" style songs YouTube Music recommends for [seedSongId]. */
        suspend fun loadRelatedSongs(seedSongId: String): Result<List<SongItem>> {
            val nextPage =
                YouTube.next(WatchEndpoint(videoId = seedSongId)).getOrElse { throwable ->
                    return Result.failure(throwable)
                }
            // The up next list is the recommendation YouTube Music actually serves for a
            // song (it is what the radio queue is built from). The related browse page is
            // only a fallback: it is a different module and often missing or empty.
            val upNextSongs = nextPage.items.filterNot { song -> song.id == seedSongId }
            if (upNextSongs.isNotEmpty()) return Result.success(upNextSongs)
            val relatedEndpoint = nextPage.relatedEndpoint ?: return Result.success(emptyList())
            return YouTube.related(relatedEndpoint).map { page -> page.songs }
        }

        private fun Song.isYouTubeRecommendationSeed(): Boolean = !song.isLocal && id.length == YOUTUBE_VIDEO_ID_LENGTH

        private companion object {
            const val QUICK_PICKS_HISTORY_WINDOW_MS = 90L * 24 * 60 * 60 * 1000
            const val YOUTUBE_VIDEO_ID_LENGTH = 11
        }
    }
