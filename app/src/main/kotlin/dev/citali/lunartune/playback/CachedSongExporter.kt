/*
 * LunarTune (2026)
 * © cognitiveshadows03 — github.com/cognitiveshadows03
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.citali.lunartune.playback

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.media3.common.C
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.TransferListener
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import dev.citali.lunartune.db.entities.Song
import timber.log.Timber

/**
 * Copies a complete Media3 cache resource into the user's music collection via MediaStore.
 *
 * The export is intentionally defensive: cache spans can be evicted or corrupted between the
 * moment the cache playlist surfaces a song and the moment the user taps export, so we:
 *   - read through [CacheDataSource] with [CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR] so a
 *     single bad span doesn't abort the whole export,
 *   - use a noop upstream that returns end-of-input instead of throwing, so cache misses
 *     produce a truncated-but-valid file rather than a [java.io.IOException],
 *   - disable cache writes during the read so we don't fight the playback cache evictor,
 *   - run on [Dispatchers.IO] (see [CachePlaylistViewModel.exportSong]) so MediaStore I/O
 *     never blocks the main thread.
 */
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
        if (!cache.isCached(song.id, 0, length)) {
            error("The audio is not fully cached")
        }

        val extension = extensionFor(format.mimeType, format.codecs)
        val mimeType = mimeTypeFor(format.mimeType)
        Timber.i("Exporting %s (%s -> %s, %d bytes) from %s", song.id, format.mimeType, extension, length, cacheName(cache))

        val resolver = context.contentResolver
        val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, "${safeName(song.song.title)}.$extension")
            put(MediaStore.Audio.Media.MIME_TYPE, mimeType)
            put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/LunarTune")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
            put(MediaStore.Audio.Media.TITLE, song.song.title)
            put(MediaStore.Audio.Media.ARTIST, song.artists.joinToString(", ") { it.name })
            put(MediaStore.Audio.Media.ALBUM, song.album?.title ?: song.song.albumName.orEmpty())
            put(MediaStore.Audio.Media.ALBUM_ARTIST, song.artists.firstOrNull()?.name.orEmpty())
            song.song.year?.let { put(MediaStore.Audio.Media.YEAR, it) }
            put(MediaStore.Audio.Media.IS_MUSIC, 1)
        }
        val uri = resolver.insert(collection, values) ?: error("Unable to create a music file")
        try {
            resolver.openOutputStream(uri)?.use { output ->
                val source = CacheDataSource.Factory()
                    .setCache(cache)
                    .setCacheWriteDataSinkFactory(null)
                    .setUpstreamDataSourceFactory(DataSource.Factory { NoopDataSource })
                    .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
                    .createDataSource()
                val dataSpec = DataSpec.Builder()
                    .setUri(buildSyntheticUri(song.id))
                    .setKey(song.id)
                    .build()
                source.open(dataSpec)
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var totalRead = 0L
                while (true) {
                    val read = source.read(buffer, 0, buffer.size)
                    if (read == C.RESULT_END_OF_INPUT) break
                    if (read == 0) continue
                    output.write(buffer, 0, read)
                    totalRead += read
                }
                source.close()
                if (totalRead != length) {
                    Timber.w("Exported %s with truncated length: %d / %d bytes", song.id, totalRead, length)
                } else {
                    Timber.i("Exported %s fully: %d bytes", song.id, totalRead)
                }
            } ?: error("Unable to open the music file")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                resolver.update(uri, ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) }, null, null)
            }
            return uri.toString()
        } catch (error: Throwable) {
            Timber.e(error, "Failed to export cached song %s", song.id)
            runCatching { resolver.delete(uri, null, null) }
            throw error
        }
    }

    private fun safeName(value: String): String =
        value.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().ifBlank { "LunarTune export" }

    private fun extensionFor(
        mimeType: String,
        codecs: String,
    ): String {
        val lowerMime = mimeType.lowercase()
        val lowerCodecs = codecs.lowercase()
        return when {
            lowerMime.contains("mp4") || lowerCodecs.contains("mp4a") || lowerCodecs.contains("aac") -> "m4a"
            lowerMime.contains("webm") && lowerCodecs.contains("opus") -> "opus"
            lowerMime.contains("webm") && lowerCodecs.contains("vorbis") -> "ogg"
            lowerMime.contains("ogg") || lowerCodecs.contains("vorbis") -> "ogg"
            lowerMime.contains("flac") || lowerCodecs.contains("flac") -> "flac"
            lowerMime.contains("mpeg") -> "mp3"
            else -> "mp3"
        }
    }

    private fun mimeTypeFor(rawMimeType: String): String {
        val base = rawMimeType.substringBefore(';').trim().lowercase()
        return when {
            base.contains("mp4") -> "audio/mp4"
            base.contains("webm") -> "audio/webm"
            base.contains("ogg") -> "audio/ogg"
            base.contains("flac") -> "audio/flac"
            base.contains("mpeg") -> "audio/mpeg"
            base.startsWith("audio/") -> base
            else -> "audio/mpeg"
        }
    }

    private fun buildSyntheticUri(songId: String): Uri =
        Uri.parse("lunartune://cache/${Uri.encode(songId)}")

    private fun cacheName(cache: Cache): String =
        if (cache === downloadCache) "downloadCache" else if (cache === playerCache) "playerCache" else "unknownCache"

    /**
     * DataSource that signals "no upstream data available" gracefully.
     *
     * Returning [C.LENGTH_UNSET] from [open] tells CacheDataSource the upstream length is
     * unknown, and returning [C.RESULT_END_OF_INPUT] from every [read] immediately signals
     * end of stream. Combined with [CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR] this means
     * cache gaps no longer throw — they just produce a truncated export instead of a failure.
     */
    private object NoopDataSource : DataSource {
        override fun open(dataSpec: DataSpec): Long = C.LENGTH_UNSET.toLong()
        override fun read(
            buffer: ByteArray,
            offset: Int,
            length: Int,
        ): Int = C.RESULT_END_OF_INPUT
        override fun getUri(): Uri? = null
        override fun close() = Unit
        override fun addTransferListener(transferListener: TransferListener) = Unit
    }
}
