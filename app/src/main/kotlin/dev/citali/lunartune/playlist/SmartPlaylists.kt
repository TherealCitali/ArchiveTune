/*
 * LunarTune (2026)
 * © cognitiveshadows03 — github.com/cognitiveshadows03
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.citali.lunartune.playlist

object SmartPlaylists {
    const val ON_REPEAT = "on_repeat"
    const val FORGOTTEN = "forgotten"
    const val RECENT = "recent"

    val ids = listOf(ON_REPEAT, FORGOTTEN, RECENT)

    fun isSmartPlaylist(id: String): Boolean = id in ids
}
