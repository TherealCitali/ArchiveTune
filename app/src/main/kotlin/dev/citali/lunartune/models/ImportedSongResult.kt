/*
 * LunarTune (2026)
 * © cognitiveshadows03 — github.com/cognitiveshadows03
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.citali.lunartune.models

import dev.citali.lunartune.db.entities.Song

enum class ImportSource {
    LOCAL,
    YOUTUBE,
    UNRESOLVED,
}

data class ImportedSongResult(
    val originalSong: Song,
    val resolvedId: String?,
    val resolvedSong: Song?,
    val source: ImportSource,
) {
    val isResolved: Boolean
        get() = source != ImportSource.UNRESOLVED && resolvedId != null && resolvedSong != null
}
