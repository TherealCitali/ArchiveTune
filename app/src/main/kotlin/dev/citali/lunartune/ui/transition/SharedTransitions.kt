/*
 * LunarTune (2026)
 * © cognitiveshadows03 — github.com/cognitiveshadows03
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.citali.lunartune.ui.transition

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.LocalSharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.rememberSharedContentState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

/**
 * Carries the current navigation destination's [AnimatedVisibilityScope] down the tree so
 * shared elements (artwork heroes) don't require threading scope parameters through every
 * screen. Provided per-destination by [sharedComposable]; null outside a destination
 * (dialogs, menus, player) where [Modifier.sharedArtwork] safely renders nothing special.
 */
val LocalNavAnimatedVisibilityScope = staticCompositionLocalOf<AnimatedVisibilityScope?> { null }

/**
 * Drop-in replacement for navigation-compose's `composable()` that additionally provides
 * [LocalNavAnimatedVisibilityScope] to the destination content. Call sites are identical.
 */
fun NavGraphBuilder.sharedComposable(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    enterTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition?)? = null,
    exitTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition?)? = null,
    popEnterTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition?)? = null,
    popExitTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition?)? = null,
    content: @Composable AnimatedVisibilityScope.(NavBackStackEntry) -> Unit,
) {
    composable(
        route = route,
        arguments = arguments,
        enterTransition = enterTransition,
        exitTransition = exitTransition,
        popEnterTransition = popEnterTransition,
        popExitTransition = popExitTransition,
    ) { entry ->
        val visibilityScope = this
        CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides visibilityScope) {
            visibilityScope.content(entry)
        }
    }
}

/**
 * Tags an artwork layout as one end of a shared-element hero. Both ends (e.g. a grid
 * thumbnail and the detail header) use the same [key]; during navigation the artwork
 * flies and morphs between them while the regular MD3 screen transition runs underneath.
 * Renders a plain modifier when [key] is null or no transition scopes are available.
 */
@Composable
fun Modifier.sharedArtwork(key: String?): Modifier {
    val transitionScope = LocalSharedTransitionScope.current
    val visibilityScope = LocalNavAnimatedVisibilityScope.current
    if (key == null || transitionScope == null || visibilityScope == null) return this
    return with(transitionScope) {
        this@sharedArtwork.sharedElement(
            state = rememberSharedContentState(key = key),
            animatedVisibilityScope = visibilityScope,
            boundsTransform = { _, _ ->
                spring(stiffness = Spring.StiffnessMediumLow)
            },
        )
    }
}

/** Shared-element key for an album's artwork, matched between lists and the detail hero. */
fun albumArtworkKey(albumId: String): String = "artwork-album-$albumId"

/** Shared-element key for a playlist's artwork, matched between lists and the detail hero. */
fun playlistArtworkKey(playlistId: String): String = "artwork-playlist-$playlistId"
