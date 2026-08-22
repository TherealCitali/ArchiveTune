/*
 * LunarTune (2026)
 * © cognitiveshadows03 — github.com/cognitiveshadows03
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.citali.lunartune.equalizer

/**
 * 10-band millibel curves inspired by Vivi Music Dolby / Dirac profiles.
 * Centers: 31, 62, 125, 250, 500, 1k, 2k, 4k, 8k, 16k Hz.
 */
object ViviEqualizerPresets {
    val chips: List<Pair<String, String>> =
        listOf(
            "dolby_open" to "Dolby Open",
            "dolby_rich" to "Dolby Rich",
            "dolby_focused" to "Dolby Focus",
            "dirac_music" to "Dirac Music",
            "dirac_movie" to "Dirac Movie",
            "dirac_game" to "Dirac Game",
        )

    fun levelsMb(id: String): List<Int>? =
        when (id) {
            // Wide, airy stage: lifted extremes, open top end.
            "dolby_open" -> listOf(350, 280, 180, 80, 40, 120, 220, 360, 450, 380)
            // Warm body: bass + low-mids, soft air.
            "dolby_rich" -> listOf(480, 400, 280, 180, 220, 160, 80, 140, 220, 120)
            // Vocal pocket: cut rumble/air, boost presence.
            "dolby_focused" -> listOf(-450, -180, 80, 260, 380, 280, 220, 80, -120, -380)
            // Music: gentle bass, rising detail toward treble.
            "dirac_music" -> listOf(280, 180, 80, 0, 40, 120, 220, 340, 460, 520)
            // Movie: LFE + dialogue shelf + surround highs.
            "dirac_movie" -> listOf(520, 400, 220, 20, 140, 200, 280, 400, 520, 480)
            // Game: punch + footsteps/cues in upper mids and highs.
            "dirac_game" -> listOf(320, 420, 260, 20, 80, 180, 380, 560, 500, 340)
            else -> null
        }
}
