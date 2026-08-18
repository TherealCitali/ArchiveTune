/*
 * LunarTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.citali.lunartune.ui.screens.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import dev.citali.lunartune.constants.PlaylistTagsFilterKey
import dev.citali.lunartune.db.MusicDatabase
import dev.citali.lunartune.db.entities.TagEntity
import dev.citali.lunartune.utils.rememberPreference

@Composable
internal fun rememberPlaylistTagFilterState(database: MusicDatabase): Pair<Set<String>, (Set<String>) -> Unit> {
    val allTags: List<TagEntity>? by database.allTags().collectAsState(initial = null)
    val (selectedTagsFilter, onSelectedTagsFilterChange) = rememberPreference(PlaylistTagsFilterKey, "")
    val selectedTagIds =
        remember(selectedTagsFilter) {
            selectedTagsFilter.toPlaylistTagIds()
        }
    val validTagIds =
        remember(allTags) {
            allTags?.map(TagEntity::id)?.toSet()
        }
    val sanitizedTagIds =
        remember(selectedTagIds, validTagIds) {
            validTagIds?.let { selectedTagIds.sanitize(validTagIds = it) } ?: selectedTagIds
        }

    LaunchedEffect(validTagIds, selectedTagIds, sanitizedTagIds, onSelectedTagsFilterChange) {
        if (validTagIds != null && selectedTagIds != sanitizedTagIds) {
            onSelectedTagsFilterChange(sanitizedTagIds.toPreferenceValue())
        }
    }

    val onSelectedTagIdsChange: (Set<String>) -> Unit =
        remember(onSelectedTagsFilterChange) {
            { tagIds -> onSelectedTagsFilterChange(tagIds.toPreferenceValue()) }
        }

    return sanitizedTagIds to onSelectedTagIdsChange
}

internal fun String.toPlaylistTagIds(): Set<String> =
    split(",")
        .asSequence()
        .map(String::trim)
        .filter(String::isNotEmpty)
        .toCollection(LinkedHashSet())

internal fun Set<String>.sanitize(validTagIds: Set<String>): Set<String> = filterTo(LinkedHashSet()) { it in validTagIds }

internal fun Set<String>.toPreferenceValue(): String = joinToString(",")
