/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 *
 * Ported and adapted for LunarTune from 4nx3b/ArchiveTune
 * (ExportDownloadedSongsScreen) — lists cached and downloaded songs and
 * exports them to a user-picked storage folder as complete audio files.
 */

package dev.citali.lunartune.ui.screens.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheSpan
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.rukamori.archivetune.innertube.YouTube
import dev.citali.lunartune.LocalDatabase
import dev.citali.lunartune.LocalDownloadUtil
import dev.citali.lunartune.LocalPlayerAwareWindowInsets
import dev.citali.lunartune.R
import dev.citali.lunartune.db.MusicDatabase
import dev.citali.lunartune.db.entities.detectAudioExtensionFromSpans
import dev.citali.lunartune.db.entities.extensionToMimeType
import dev.citali.lunartune.playback.AudioTagger
import dev.citali.lunartune.ui.component.IconButton
import dev.citali.lunartune.ui.utils.backToMain

private data class CachedSongRow(
    val songId: String,
    val cacheKey: String,
    val isDownloaded: Boolean,
    @param:StringRes val sourceLabelRes: Int,
    val title: String,
    val artist: String,
    val thumbnailUrl: String?,
)

private const val EXPORT_COPY_BUFFER_BYTES = 1024 * 1024

/**
 * Collects every song held in the download cache ("Downloaded") or the player
 * cache ("Cached") and copies complete songs out to a user-chosen folder,
 * preserving the original audio format and embedding metadata where the
 * container supports it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportSongsScreen(navController: NavController) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val downloadUtil = LocalDownloadUtil.current
    val coroutineScope = rememberCoroutineScope()

    var songs by remember { mutableStateOf<List<CachedSongRow>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isExporting by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var exportedCount by remember { mutableIntStateOf(0) }
    var deletedCount by remember { mutableIntStateOf(0) }
    var totalCount by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    val selectedIds: SnapshotStateList<String> = remember { mutableStateListOf() }

    val displayedSongs = remember(songs, searchQuery) {
        if (searchQuery.isBlank()) songs
        else {
            val q = searchQuery.lowercase().trim()
            songs.filter {
                it.title.lowercase().contains(q) || it.artist.lowercase().contains(q)
            }
        }
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val downloadCache = downloadUtil.downloadCache
            val playerCache = downloadUtil.playerCache

            val rows = (downloadCache.keys + playerCache.keys)
                .distinct()
                .mapNotNull { key ->
                    val isDownloaded = downloadCache.keys.contains(key)
                    val cache: Cache = if (isDownloaded) downloadCache else playerCache
                    val spans = runCatching { cache.getCachedSpans(key) }.getOrNull().orEmpty()
                    if (spans.isEmpty()) return@mapNotNull null
                    // Lossless streams cache under a mono: key prefix; strip it
                    // so the song lookup finds the real entry.
                    val songId = key.removePrefix("mono:")
                    val songEntity = database.getSongByIdBlocking(songId)
                    val title = songEntity?.song?.title?.takeIf { it.isNotBlank() }
                        ?: "Unknown song ($key)"
                    val artist = songEntity?.artists?.firstOrNull()?.name?.takeIf { it.isNotBlank() }
                        ?: songEntity?.album?.title?.takeIf { it.isNotBlank() }
                        ?: ""
                    CachedSongRow(
                        songId = songId,
                        cacheKey = key,
                        isDownloaded = isDownloaded,
                        sourceLabelRes = if (isDownloaded) R.string.downloaded_desc else R.string.cached,
                        title = title,
                        artist = artist,
                        thumbnailUrl = songEntity?.song?.thumbnailUrl,
                    )
                }.sortedWith(compareBy({ it.title.lowercase() }, { it.isDownloaded }))
            songs = rows
            isLoading = false
        }
    }

    val pickFolderLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { treeUri ->
            if (treeUri == null) return@rememberLauncherForActivityResult
            val toExport = songs.filter { it.cacheKey in selectedIds }
            if (toExport.isEmpty()) {
                Toast.makeText(
                    context,
                    context.getString(R.string.export_songs_pick_folder_first),
                    Toast.LENGTH_SHORT,
                ).show()
                return@rememberLauncherForActivityResult
            }
            isExporting = true
            totalCount = toExport.size
            exportedCount = 0
            coroutineScope.launch {
                var exported = 0
                var failed = 0
                try {
                    withContext(Dispatchers.IO) {
                        val downloadCache = downloadUtil.downloadCache
                        val playerCache = downloadUtil.playerCache
                        val parentDocUri =
                            android.provider.DocumentsContract.buildDocumentUriUsingTree(
                                treeUri,
                                android.provider.DocumentsContract.getTreeDocumentId(treeUri),
                            )
                        val tempDir = java.io.File(context.cacheDir, "export_tmp").apply { mkdirs() }
                        loop@ for (row in toExport) {
                            val cache: Cache =
                                if (downloadCache.keys.contains(row.cacheKey)) downloadCache else playerCache
                            val spans = runCatching { cache.getCachedSpans(row.cacheKey) }.getOrNull()

                            if (spans.isNullOrEmpty()) { failed++; continue@loop }
                            val totalSpanBytes = spans.sumOf { it.length }
                            if (totalSpanBytes <= 0L) { failed++; continue@loop }
                            val sourceExt = detectAudioExtensionFromSpans(spans)
                            val safeTitle =
                                row.title
                                    .replace(Regex("[\\\\/:*?\"<>|]"), "_")
                                    .ifBlank { "audio_${row.songId}" }

                            val tempFile =
                                java.io.File(tempDir, "${row.songId}_${row.cacheKey.hashCode()}.$sourceExt")
                            val mp3File =
                                java.io.File(tempDir, "${row.songId}_${row.cacheKey.hashCode()}.mp3")
                            try {
                                runCatching {
                                    java.io.FileOutputStream(tempFile).use { output ->
                                        val outBuf =
                                            java.io.BufferedOutputStream(output, EXPORT_COPY_BUFFER_BYTES)
                                        spans.sortedBy { it.position }.forEach { span: CacheSpan ->
                                            span.file?.let { file ->
                                                java.io.FileInputStream(file).use { input ->
                                                    java.io.BufferedInputStream(input, EXPORT_COPY_BUFFER_BYTES)
                                                        .use { bufIn -> bufIn.copyTo(outBuf) }
                                                }
                                            }
                                        }
                                        outBuf.flush()
                                    }
                                }.getOrElse {
                                    tempFile.delete()
                                    failed++
                                    continue@loop
                                }

                                // WebM/Opus streams cannot carry tags in their source
                                // container, so convert them to MP3 — the most widely
                                // supported tagged format — before export.
                                var exportFile = tempFile
                                var exportExt = sourceExt
                                if (sourceExt == "webm" || sourceExt == "opus") {
                                    val session = runCatching {
                                        FFmpegKit.executeWithArguments(
                                            arrayOf(
                                                "-y", "-hide_banner", "-loglevel", "error",
                                                "-i", tempFile.absolutePath,
                                                "-vn",
                                                "-c:a", "libmp3lame",
                                                "-b:a", "320k",
                                                mp3File.absolutePath,
                                            ),
                                        )
                                    }.getOrNull()
                                    if (session != null &&
                                        ReturnCode.isSuccess(session.returnCode) &&
                                        mp3File.exists() && mp3File.length() > 0L
                                    ) {
                                        exportFile = mp3File
                                        exportExt = "mp3"
                                    }
                                    // On failure fall back to the raw WebM/Opus copy so
                                    // the song is still exported (without embedded tags).
                                }

                                val mime = extensionToMimeType(exportExt)
                                val resolvedMetadata = resolveExportMetadata(database, row)
                                AudioTagger.tag(exportFile, resolvedMetadata)

                                val destUri =
                                    android.provider.DocumentsContract.createDocument(
                                        context.contentResolver,
                                        parentDocUri,
                                        mime,
                                        "$safeTitle.$exportExt",
                                    ) ?: run { failed++; continue@loop }
                                runCatching {
                                    context.contentResolver.openOutputStream(destUri, "w")?.use { output ->
                                        java.io.BufferedOutputStream(output, EXPORT_COPY_BUFFER_BYTES)
                                            .use { bufOut ->
                                                java.io.FileInputStream(exportFile).use { input ->
                                                    java.io
                                                        .BufferedInputStream(input, EXPORT_COPY_BUFFER_BYTES)
                                                        .use { bufIn -> bufIn.copyTo(bufOut) }
                                                }
                                            }
                                    }
                                }.onSuccess {
                                    exported++
                                    exportedCount = exported
                                }.onFailure { failed++ }
                            } finally {
                                tempFile.delete()
                                mp3File.delete()
                            }
                        }

                        runCatching { tempDir.listFiles()?.forEach { it.delete() } }
                    }
                } finally {
                    isExporting = false
                }
                val failedMsg = if (failed > 0) ", $failed failed" else ""
                Toast.makeText(
                    context,
                    context.getString(
                        R.string.export_songs_complete,
                        exported,
                        failedMsg,
                    ),
                    Toast.LENGTH_LONG,
                ).show()
            }
        }

    val allSelected = displayedSongs.isNotEmpty() && selectedIds.size == displayedSongs.size

    fun deleteSelected() {
        val toDelete = songs.filter { it.cacheKey in selectedIds }
        if (toDelete.isEmpty()) return
        isDeleting = true
        totalCount = toDelete.size
        deletedCount = 0
        coroutineScope.launch {
            var deleted = 0
            var failed = 0
            try {
                withContext(Dispatchers.IO) {
                    val downloadCache = downloadUtil.downloadCache
                    val playerCache = downloadUtil.playerCache
                    for (row in toDelete) {
                        var removed = false
                        runCatching { downloadCache.removeResource(row.cacheKey) }.onSuccess { removed = true }
                        runCatching { playerCache.removeResource(row.cacheKey) }.onSuccess { removed = true }
                        if (removed) deleted++ else failed++
                        deletedCount = deleted
                    }

                    runCatching {
                        toDelete.forEach { row ->
                            downloadUtil.downloadManager.removeDownload(row.cacheKey)
                        }
                    }
                }
            } finally {
                isDeleting = false
            }

            val failedMsg = if (failed > 0) ", $failed failed" else ""
            Toast.makeText(
                context,
                context.getString(
                    R.string.export_songs_delete_complete,
                    deleted,
                    failedMsg,
                ),
                Toast.LENGTH_LONG,
            ).show()

            val deletedKeys = toDelete.map { it.cacheKey }.toSet()
            songs = songs.filterNot { it.cacheKey in deletedKeys }
            selectedIds.removeAll(deletedKeys)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.export_songs)) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (isSearchActive) {
                                isSearchActive = false
                                searchQuery = ""
                            } else {
                                navController.navigateUp()
                            }
                        },
                        onLongClick = navController::backToMain,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = null,
                        )
                    }
                },
                actions = {
                    if (!isSearchActive && songs.isNotEmpty()) {
                        IconButton(
                            onClick = { isSearchActive = true },
                            onLongClick = {},
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.search),
                                contentDescription = stringResource(R.string.search),
                            )
                        }
                        IconButton(
                            onClick = {
                                if (allSelected) selectedIds.clear()
                                else {
                                    selectedIds.clear()
                                    selectedIds.addAll(displayedSongs.map { it.cacheKey })
                                }
                            },
                            onLongClick = {},
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (allSelected) R.drawable.deselect else R.drawable.select_all,
                                ),
                                contentDescription = null,
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (songs.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 4.dp,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(
                                LocalPlayerAwareWindowInsets.current.only(
                                    WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
                                ),
                            ).padding(16.dp),
                    ) {
                        Text(
                            text = stringResource(
                                R.string.export_songs_selected_count,
                                selectedIds.size,
                                displayedSongs.size,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                        )
                        if (isExporting || isDeleting) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = if (isExporting) {
                                    stringResource(
                                        R.string.export_songs_progress,
                                        exportedCount,
                                        totalCount,
                                    )
                                } else {
                                    stringResource(
                                        R.string.export_songs_delete_progress,
                                        deletedCount,
                                        totalCount,
                                    )
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedButton(
                                onClick = { showDeleteConfirm = true },
                                enabled = !isExporting && !isDeleting && selectedIds.isNotEmpty(),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error,
                                ),
                                modifier = Modifier.weight(1f),
                            ) {
                                if (isDeleting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                    )
                                    Spacer(Modifier.width(8.dp))
                                } else {
                                    Icon(
                                        painter = painterResource(R.drawable.delete),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                }
                                Text(stringResource(R.string.export_songs_delete))
                            }
                            FilledTonalButton(
                                onClick = { pickFolderLauncher.launch(null) },
                                enabled = !isExporting && !isDeleting && selectedIds.isNotEmpty(),
                                modifier = Modifier.weight(1f),
                            ) {
                                if (isExporting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                    )
                                    Spacer(Modifier.width(8.dp))
                                } else {
                                    Icon(
                                        painter = painterResource(R.drawable.send),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                }
                                Text(stringResource(R.string.export_songs_pick_folder))
                            }
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
            }
            songs.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_download),
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.export_songs_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            displayedSongs.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.search_off),
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.export_songs_search_empty, searchQuery),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = innerPadding.calculateTopPadding(),
                        bottom = innerPadding.calculateBottomPadding() + 120.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(displayedSongs, key = { it.cacheKey }) { row ->
                        val isSelected = row.cacheKey in selectedIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isSelected) selectedIds.remove(row.cacheKey)
                                    else selectedIds.add(row.cacheKey)
                                }.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (!row.thumbnailUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = row.thumbnailUrl,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop,
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(R.drawable.music_note),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = row.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (row.artist.isNotBlank()) {
                                    Text(
                                        text = row.artist,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Text(
                                    text = stringResource(row.sourceLabelRes),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        },
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isSelected) {
                                    Icon(
                                        painter = painterResource(R.drawable.check),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.export_songs_delete_confirm_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.export_songs_delete_confirm_message,
                        selectedIds.size,
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        deleteSelected()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(stringResource(R.string.export_songs_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
}

private fun fetchArtworkBytes(url: String): ByteArray? = runCatching {
    val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
    connection.connectTimeout = 10_000
    connection.readTimeout = 15_000
    connection.requestMethod = "GET"
    connection.setRequestProperty("User-Agent", "LunarTune")
    connection.instanceFollowRedirects = true
    connection.useCaches = true
    try {
        val responseCode = connection.responseCode
        if (responseCode !in 200..299) return@runCatching null
        val contentType = connection.contentType ?: ""

        if (!contentType.startsWith("image/")) return@runCatching null
        connection.inputStream.use { it.readBytes() }
    } finally {
        connection.disconnect()
    }
}.getOrNull()

private suspend fun resolveExportMetadata(
    database: MusicDatabase,
    row: CachedSongRow,
): AudioTagger.Metadata {
    val songEntity = database.getSongByIdBlocking(row.songId)

    val dbTitle = songEntity?.song?.title?.takeIf(String::isNotBlank)
    val title = dbTitle ?: row.title.takeIf { it.isNotBlank() }

    val dbArtists = songEntity?.artists?.mapNotNull { it.name.takeIf(String::isNotBlank) }
        ?.takeIf { it.isNotEmpty() }
    val dbArtistStr = dbArtists?.joinToString(", ")

    val dbAlbum = songEntity?.album?.title?.takeIf(String::isNotBlank)
        ?: songEntity?.song?.albumName?.takeIf(String::isNotBlank)

    val dbYear = songEntity?.song?.year?.takeIf { it > 0 }

    val dbThumb = songEntity?.song?.thumbnailUrl?.takeIf(String::isNotBlank)
    val thumbUrl = dbThumb ?: row.thumbnailUrl?.takeIf(String::isNotBlank)

    val hasFullMetadata = title != null && !dbArtistStr.isNullOrBlank() && thumbUrl != null
    if (hasFullMetadata) {
        val artworkBytes = thumbUrl?.let { fetchArtworkBytes(it) }
        return AudioTagger.Metadata(
            title = title,
            artist = dbArtistStr,
            albumArtist = dbArtists?.firstOrNull(),
            album = dbAlbum,
            year = dbYear,
            artworkBytes = artworkBytes,
        )
    }

    val mediaInfo = runCatching {
        YouTube.getMediaInfo(row.songId).getOrNull()
    }.getOrNull()

    val resolvedTitle = title
        ?: mediaInfo?.title?.takeIf(String::isNotBlank)
        ?: row.title
    val resolvedArtist = dbArtistStr
        ?: mediaInfo?.author?.takeIf(String::isNotBlank)
        ?: ""

    val resolvedThumb = thumbUrl
        ?: "https://i.ytimg.com/vi/${row.songId}/hqdefault.jpg"

    val artworkBytes = resolvedThumb.let { fetchArtworkBytes(it) }

    return AudioTagger.Metadata(
        title = resolvedTitle?.takeIf(String::isNotBlank),
        artist = resolvedArtist.takeIf(String::isNotBlank),
        albumArtist = (dbArtists?.firstOrNull() ?: mediaInfo?.author)?.takeIf(String::isNotBlank),
        album = dbAlbum,
        year = dbYear,
        artworkBytes = artworkBytes,
    )
}
