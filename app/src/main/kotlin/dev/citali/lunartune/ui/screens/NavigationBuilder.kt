/*
 * LunarTune (2026)
 * © cognitiveshadows03 — github.com/cognitiveshadows03
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.citali.lunartune.ui.screens

import android.net.Uri
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import dev.citali.lunartune.BuildConfig
import dev.citali.lunartune.constants.UpdateChannel
import dev.citali.lunartune.defaultUpdateChannel
import dev.citali.lunartune.musicrecognition.MusicRecognitionRoute
import dev.citali.lunartune.musicrecognition.MusicRecognitionDetailsRoute
import dev.citali.lunartune.ui.screens.BrowseScreen
import dev.citali.lunartune.ui.screens.artist.ArtistAlbumsScreen
import dev.citali.lunartune.ui.screens.artist.ArtistItemsScreen
import dev.citali.lunartune.ui.screens.artist.ArtistScreen
import dev.citali.lunartune.ui.screens.artist.ArtistSongsScreen
import dev.citali.lunartune.ui.screens.library.LibraryScreen
import dev.citali.lunartune.ui.screens.library.LocalSongScreen
import dev.citali.lunartune.ui.screens.musicrecognition.MusicRecognitionScreen
import dev.citali.lunartune.ui.screens.musicrecognition.MusicRecognitionDetailsScreen
import dev.citali.lunartune.ui.screens.playlist.AutoPlaylistScreen
import dev.citali.lunartune.ui.screens.playlist.CachePlaylistScreen
import dev.citali.lunartune.ui.screens.playlist.LocalPlaylistScreen
import dev.citali.lunartune.ui.screens.playlist.OnlinePlaylistScreen
import dev.citali.lunartune.ui.screens.playlist.SpotifyPlaylistScreen
import dev.citali.lunartune.ui.screens.playlist.TopPlaylistScreen
import dev.citali.lunartune.ui.screens.search.OnlineSearchResult
import dev.citali.lunartune.ui.screens.search.OnlineSearchResultArgument
import dev.citali.lunartune.ui.screens.search.OnlineSearchResultRoute
import dev.citali.lunartune.ui.screens.search.OnlineSearchResultRoutePrefix
import dev.citali.lunartune.ui.screens.search.SearchScreen
import dev.citali.lunartune.ui.screens.settings.AboutScreen
import dev.citali.lunartune.ui.screens.settings.AccountSettings
import dev.citali.lunartune.ui.screens.settings.AiIntegrationSettings
import dev.citali.lunartune.ui.screens.settings.AodCustomizedScreen
import dev.citali.lunartune.ui.screens.settings.AppearanceSettings
import dev.citali.lunartune.ui.screens.settings.BackupAndRestore
import dev.citali.lunartune.ui.screens.settings.AppLockScreen
import dev.citali.lunartune.ui.screens.settings.PinSetupScreen
import dev.citali.lunartune.ui.screens.settings.ChangelogScreen
import dev.citali.lunartune.ui.screens.settings.ChiperSettings
import dev.citali.lunartune.ui.screens.settings.ContentSettings
import dev.citali.lunartune.ui.screens.settings.CustomizeBackground
import dev.citali.lunartune.ui.screens.settings.DebugSettings
import dev.citali.lunartune.ui.screens.settings.DiscordSettings
import dev.citali.lunartune.ui.screens.settings.HiddenPlaylistsScreen
import dev.citali.lunartune.ui.screens.settings.IntegrationScreen
import dev.citali.lunartune.ui.screens.settings.InternetSettings
import dev.citali.lunartune.ui.screens.settings.LastFMSettings
import dev.citali.lunartune.ui.screens.settings.LogcatScreen
import dev.citali.lunartune.ui.screens.settings.LyricsAnimationSettings
import dev.citali.lunartune.ui.screens.settings.LyricsSettings
import dev.citali.lunartune.ui.screens.settings.NavigationBarSettings
import dev.citali.lunartune.ui.screens.settings.MusicTogetherScreen
import dev.citali.lunartune.ui.screens.settings.PalettePickerScreen
import dev.citali.lunartune.ui.screens.settings.PlayerSettings
import dev.citali.lunartune.ui.screens.settings.PrivacySettings
import dev.citali.lunartune.ui.screens.settings.SettingsScreen
import dev.citali.lunartune.ui.screens.settings.StorageSettings
import dev.citali.lunartune.ui.screens.settings.StreamSourcesSettings
import dev.citali.lunartune.ui.screens.settings.ThemeCreatorScreen
import dev.citali.lunartune.ui.screens.settings.UpdateScreen
import dev.citali.lunartune.ui.transition.sharedComposable
import dev.citali.lunartune.viewmodels.OnlineSearchSort

@OptIn(ExperimentalMaterial3Api::class)
fun NavGraphBuilder.navigationBuilder(
    navController: NavHostController,
    scrollBehavior: TopAppBarScrollBehavior,
    latestVersionName: () -> String,
    disableAnimations: Boolean = false,
    onClearUpdateBadge: () -> Unit = {},
    homeScrollConnection: NestedScrollConnection? = null,
    searchScrollConnection: NestedScrollConnection? = null,
    onlineSearchSort: OnlineSearchSort = OnlineSearchSort.DEFAULT,
) {
    sharedComposable(Screens.Home.route) {
        HomeScreen(navController, headerScrollConnection = homeScrollConnection)
    }
    sharedComposable(
        Screens.Library.route,
    ) {
        LibraryScreen(navController)
    }
    sharedComposable(Screens.Search.route) {
        SearchScreen(
            navController = navController,
            onSearchClick = {
                navController.currentBackStackEntry
                    ?.savedStateHandle
                    ?.set("openSearch", true)
            },
            headerScrollConnection = searchScrollConnection,
        )
    }
    sharedComposable("local_songs") {
        LocalSongScreen(navController)
    }
    sharedComposable("history") {
        HistoryScreen(navController)
    }
    sharedComposable("stats") {
        StatsScreen(navController)
    }
    sharedComposable("news") {
        NewsScreen(navController)
    }
    sharedComposable(
        route = "view_news/{newsId}",
        arguments =
            listOf(
                navArgument("newsId") { type = NavType.StringType },
            ),
    ) {
        ViewNewsScreen(navController)
    }
    sharedComposable(
        route = "year_in_music?year={year}",
        arguments =
            listOf(
                navArgument("year") {
                    type = NavType.IntType
                    defaultValue = -1
                },
            ),
    ) { backStackEntry ->
        val selectedYear = backStackEntry.arguments?.getInt("year")?.takeIf { it > 0 }
        YearInMusicScreen(
            navController = navController,
            initialYear = selectedYear,
        )
    }
    sharedComposable(MusicRecognitionRoute) {
        MusicRecognitionScreen(navController)
    }
    sharedComposable(MusicRecognitionDetailsRoute) { backStackEntry ->
        val encodedTrack = backStackEntry.arguments?.getString("encodedTrack").orEmpty()
        MusicRecognitionDetailsScreen(navController, encodedTrack)
    }
    sharedComposable(Screens.MoodAndGenres.route) {
        MoodAndGenresScreen(navController)
    }
    sharedComposable("account") {
        AccountScreen(navController, scrollBehavior)
    }
    sharedComposable("new_release") {
        NewReleaseScreen(navController, scrollBehavior)
    }
    sharedComposable("charts_screen") {
        ChartsScreen(navController)
    }
    sharedComposable(
        route = "browse/{browseId}",
        arguments =
            listOf(
                navArgument("browseId") {
                    type = NavType.StringType
                },
            ),
    ) {
        BrowseScreen(
            navController,
            scrollBehavior,
            it.arguments?.getString("browseId"),
        )
    }
    sharedComposable(
        route = OnlineSearchResultRoute,
        arguments =
            listOf(
                navArgument(OnlineSearchResultArgument) {
                    type = NavType.StringType
                },
            ),
        enterTransition = {
            if (disableAnimations) {
                fadeIn(tween(0))
            } else {
                fadeIn(tween(250))
            }
        },
        exitTransition = {
            if (disableAnimations) {
                fadeOut(tween(0))
            } else if (targetState.destination.route?.startsWith(OnlineSearchResultRoutePrefix) == true) {
                fadeOut(tween(200))
            } else {
                fadeOut(tween(200)) + slideOutHorizontally { -it / 2 }
            }
        },
        popEnterTransition = {
            if (disableAnimations) {
                fadeIn(tween(0))
            } else if (initialState.destination.route?.startsWith(OnlineSearchResultRoutePrefix) == true) {
                fadeIn(tween(250))
            } else {
                fadeIn(tween(250)) + slideInHorizontally { -it / 2 }
            }
        },
        popExitTransition = {
            if (disableAnimations) {
                fadeOut(tween(0))
            } else {
                fadeOut(tween(200))
            }
        },
    ) {
        OnlineSearchResult(
            navController = navController,
            searchSort = onlineSearchSort,
        )
    }
    sharedComposable(
        route = "album/{albumId}",
        arguments =
            listOf(
                navArgument("albumId") {
                    type = NavType.StringType
                },
            ),
    ) {
        AlbumScreen(navController, scrollBehavior)
    }
    sharedComposable(
        route = "artist/{artistId}",
        arguments =
            listOf(
                navArgument("artistId") {
                    type = NavType.StringType
                },
            ),
    ) {
        ArtistScreen(navController, scrollBehavior)
    }
    sharedComposable(
        route = "artist/{artistId}/songs",
        arguments =
            listOf(
                navArgument("artistId") {
                    type = NavType.StringType
                },
            ),
    ) {
        ArtistSongsScreen(navController, scrollBehavior)
    }
    sharedComposable(
        route = "artist/{artistId}/albums",
        arguments =
            listOf(
                navArgument("artistId") {
                    type = NavType.StringType
                },
            ),
    ) {
        ArtistAlbumsScreen(navController, scrollBehavior)
    }
    sharedComposable(
        route = "artist/{artistId}/items?browseId={browseId}&params={params}",
        arguments =
            listOf(
                navArgument("artistId") {
                    type = NavType.StringType
                },
                navArgument("browseId") {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument("params") {
                    type = NavType.StringType
                    nullable = true
                },
            ),
    ) {
        ArtistItemsScreen(navController, scrollBehavior)
    }
    sharedComposable(
        route = "online_playlist/{playlistId}",
        arguments =
            listOf(
                navArgument("playlistId") {
                    type = NavType.StringType
                },
            ),
    ) {
        OnlinePlaylistScreen(navController, scrollBehavior)
    }
    sharedComposable(
        route = "local_playlist/{playlistId}",
        arguments =
            listOf(
                navArgument("playlistId") {
                    type = NavType.StringType
                },
            ),
    ) {
        LocalPlaylistScreen(navController, scrollBehavior)
    }
    sharedComposable(
        route = "spotify_playlist/{playlistId}",
        arguments =
            listOf(
                navArgument("playlistId") {
                    type = NavType.StringType
                },
            ),
    ) {
        SpotifyPlaylistScreen(navController, scrollBehavior)
    }
    sharedComposable(
        route = "auto_playlist/{playlist}?tab={tab}",
        arguments =
            listOf(
                navArgument("playlist") {
                    type = NavType.StringType
                },
                navArgument("tab") {
                    type = NavType.StringType
                    defaultValue = "downloaded"
                },
            ),
    ) {
        AutoPlaylistScreen(navController, scrollBehavior)
    }
    sharedComposable(
        route = "cache_playlist/{playlist}",
        arguments =
            listOf(
                navArgument("playlist") {
                    type = NavType.StringType
                },
            ),
    ) {
        CachePlaylistScreen(navController, scrollBehavior)
    }
    sharedComposable(
        route = "top_playlist/{top}",
        arguments =
            listOf(
                navArgument("top") {
                    type = NavType.StringType
                },
            ),
    ) {
        TopPlaylistScreen(navController, scrollBehavior)
    }
    sharedComposable(
        route = "youtube_browse/{browseId}?params={params}",
        arguments =
            listOf(
                navArgument("browseId") {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument("params") {
                    type = NavType.StringType
                    nullable = true
                },
            ),
    ) {
        YouTubeBrowseScreen(navController)
    }
    sharedComposable("settings") {
        SettingsScreen(navController, latestVersionName())
    }
    sharedComposable("settings/account") {
        AccountSettings(navController, latestVersionName())
    }
    sharedComposable("settings/hidden_playlists") {
        HiddenPlaylistsScreen(navController)
    }
    sharedComposable("settings/appearance") {
        AppearanceSettings(navController)
    }
    sharedComposable("settings/appearance/navigation_bar") {
        NavigationBarSettings(navController)
    }
    sharedComposable("settings/appearance/aod_customized") {
        AodCustomizedScreen(navController)
    }
    sharedComposable("settings/appearance/palette_picker") {
        PalettePickerScreen(navController)
    }
    sharedComposable("settings/appearance/lyrics_animations") {
        LyricsAnimationSettings(navController)
    }
    sharedComposable("settings/appearance/theme_creator") {
        ThemeCreatorScreen(navController)
    }
    sharedComposable("settings/content") {
        ContentSettings(navController)
    }
    sharedComposable("settings/lyrics") {
        LyricsSettings(navController)
    }
    sharedComposable("settings/internet") {
        InternetSettings(navController)
    }
    sharedComposable("settings/player") {
        PlayerSettings(navController)
    }
    sharedComposable("settings/player/stream_sources") {
        StreamSourcesSettings(navController)
    }
    sharedComposable("settings/player/chiper") {
        ChiperSettings(navController)
    }
    sharedComposable("settings/storage") {
        StorageSettings(navController)
    }
    sharedComposable("settings/privacy") {
        PrivacySettings(navController)
    }
    sharedComposable("settings/app_lock") {
        AppLockScreen(navController)
    }
    sharedComposable("settings/pin_setup") {
        PinSetupScreen(navController)
    }
    sharedComposable("settings/backup_restore") {
        BackupAndRestore(navController)
    }
    sharedComposable("settings/discord") {
        DiscordSettings(navController)
    }
    sharedComposable("settings/integration") {
        IntegrationScreen(navController)
    }
    sharedComposable("settings/ai_integration") {
        AiIntegrationSettings(navController)
    }
    sharedComposable("settings/music_together") {
        MusicTogetherScreen(navController)
    }
    sharedComposable("settings/lastfm") {
        LastFMSettings(navController)
    }
    sharedComposable("settings/discord/experimental") {
        dev.citali.lunartune.ui.screens.settings
            .DiscordExperimental(navController)
    }
    sharedComposable("settings/misc") {
        DebugSettings(navController)
    }
    sharedComposable("settings/logcat") {
        LogcatScreen(navController)
    }
    if (BuildConfig.UPDATER_AVAILABLE) {
        sharedComposable("settings/update") {
            UpdateScreen(navController, onUpToDate = onClearUpdateBadge)
        }
    }
    sharedComposable(
        route = "settings/changelog?channel={channel}",
        arguments =
            listOf(
                navArgument("channel") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
    ) { backStackEntry ->
        val channelName = backStackEntry.arguments?.getString("channel")
        val channel = UpdateChannel.fromStoredName(channelName, defaultUpdateChannel)
        ChangelogScreen(navController, channel = channel)
    }
    sharedComposable("settings/about") {
        AboutScreen(navController)
    }
    sharedComposable("customize_background") {
        CustomizeBackground(navController)
    }
    sharedComposable(
        route = "$LOGIN_ROUTE?$LOGIN_URL_ARGUMENT={$LOGIN_URL_ARGUMENT}",
        arguments =
            listOf(
                navArgument(LOGIN_URL_ARGUMENT) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
    ) { backStackEntry ->
        LoginScreen(
            navController,
            startUrl = backStackEntry.arguments?.getString(LOGIN_URL_ARGUMENT)?.let(Uri::decode),
        )
    }
}
