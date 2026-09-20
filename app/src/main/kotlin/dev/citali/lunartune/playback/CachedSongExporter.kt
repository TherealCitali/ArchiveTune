/*
 * LunarTune (2026)
 * © cognitiveshadows03 — github.com/cognitiveshadows03
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.citali.lunartune.playback

import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.upstream.DataSource
import dev.citali.lunartune.db.entities.Song
import java.io.IOException

/** Copies a complete Media3 cache resource into the user's music collection. */
class CachedSongExporter(
    private val context: Context,
    private val playerCache: Cache,
    private val downloadCache: Cache,
) {
    fun export(song: Song): String {
        val format = song.format ?: error("This song has no audio format")
        val length = format.contentLength
        require(length > 0) { "The cached audio has no known length" }
        val cache = when {
            downloadCache.keys.contains(song.id) -> downloadCache
            playerCache.keys.contains(song.id) -> playerCache
            else -> error("The audio is not cached")
        }
        require(cache.isCached(song.id, 0, length)) { "The audio is not fully cached" }

        val extension = when {
            format.mimeType.contains("mp4", true) || format.codecs.contains("mp4", true) -> "m4a"
            format.mimeType.contains("webm", true) || format.codecs.contains("opus", true) -> "opus"
            format.mimeType.contains("ogg", true) -> "ogg"
            format.mimeType.contains("flac", true) -> "flac"
            else -> "mp3"
        }
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, "${safeName(song.song.title)}.$extension")
            put(MediaStore.Audio.Media.MIME_TYPE, format.mimeType.substringBefore(';'))
            put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/LunarTune")
            put(MediaStore.Audio.Media.IS_PENDING, 1)
            put(MediaStore.Audio.Media.TITLE, song.song.title)
            put(MediaStore.Audio.Media.ARTIST, song.artists.joinToString(", ") { it.name })
            put(MediaStore.Audio.Media.ALBUM, song.album?.title ?: song.song.albumName.orEmpty())
            put(MediaStore.Audio.Media.ALBUM_ARTIST, song.artists.firstOrNull()?.name.orEmpty())
            song.song.year?.let { put(MediaStore.Audio.Media.YEAR, it) }
            put(MediaStore.Audio.Media.IS_MUSIC, 1)
        }
        val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("Unable to create a music file")
        try {
            resolver.openOutputStream(uri)?.use { output ->
                val source = CacheDataSource.Factory()
                    .setCache(cache)
                    .setUpstreamDataSourceFactory(DataSource.Factory { throw IOException("cache miss") })
                    .createDataSource()
                source.open(DataSpec.Builder().setUri(song.id).setKey(song.id).setLength(length).build())
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = source.read(buffer, 0, buffer.size)
                    if (read == -1) break
                    output.write(buffer, 0, read)
                }
                source.close()
            } ?: error("Unable to open the music file")
            resolver.update(uri, ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) }, null, null)
            return uri.toString()
        } catch (error: Throwable) {
            resolver.delete(uri, null, null)
            throw error
        }
    }

    private fun safeName(value: String): String =
        value.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().ifBlank { "LunarTune export" }
}
