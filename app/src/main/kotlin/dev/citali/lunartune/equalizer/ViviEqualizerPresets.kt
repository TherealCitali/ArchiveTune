/*
 * LunarTune (2026)
 * © cognitiveshadows03 — github.com/cognitiveshadows03
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.citali.lunartune.equalizer

/**
 * 10-band millibel curves from Vivi Music (vivizzz007/vivi-music).
 */
object ViviEqualizerPresets {
    val chips: List<Pair<String, String>> =
        listOf(
            "dolby_open" to "Dolby Open",
            "dolby_rich" to "Dolby Rich",
            "dolby_focused" to "Dolby Focused",
            "dirac_music" to "Dirac Music",
            "dirac_movie" to "Dirac Movie",
            "dirac_game" to "Dirac Game",
        )

    fun levelsMb(id: String): List<Int>? =
        when (id) {
            "dolby_open" -> listOf(150, 180, 220, 180, 160, 210, 250, 280, 180, 80)
            "dolby_rich" -> listOf(100, 160, 200, 220, 280, 260, 240, 200, 150, 50)
            "dolby_focused" -> listOf(-300, -50, 130, 180, 220, 120, 140, 100, -50, -300)
            "dirac_music" -> listOf(200, 140, 80, 0, 30, 80, 140, 200, 280, 350)
            "dirac_movie" -> listOf(300, 250, 150, 0, 70, 120, 180, 250, 320, 400)
            "dirac_game" -> listOf(150, 250, 200, 0, 80, 150, 300, 450, 400, 280)
            else -> null
        }
}
