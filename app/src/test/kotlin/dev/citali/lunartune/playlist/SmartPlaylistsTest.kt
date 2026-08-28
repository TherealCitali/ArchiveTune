/*
 * LunarTune (2026)
 * © cognitiveshadows03 — github.com/cognitiveshadows03
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.citali.lunartune.playlist

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartPlaylistsTest {
    @Test
    fun knownSmartPlaylists_areRecognized() {
        assertTrue(SmartPlaylists.isSmartPlaylist("on_repeat"))
        assertTrue(SmartPlaylists.isSmartPlaylist("forgotten"))
        assertTrue(SmartPlaylists.isSmartPlaylist("recent"))
        assertFalse(SmartPlaylists.isSmartPlaylist("liked"))
    }
}
