/*
 * LunarTune (2026)
 * © cognitiveshadows03 — github.com/cognitiveshadows03
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.citali.lunartune.ui.menu

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import dev.citali.lunartune.R
import dev.citali.lunartune.constants.NeverRecommendSongIdsKey
import dev.citali.lunartune.constants.PerSongAlbumArtOverridesKey
import dev.citali.lunartune.constants.PerSongTagsKey
import dev.citali.lunartune.constants.PlayerDesignStyle
import dev.citali.lunartune.constants.PlayerDesignStyleKey
import dev.citali.lunartune.constants.PlayerDesignStyleOverridesKey
import dev.citali.lunartune.ui.component.ListDialog
import dev.citali.lunartune.utils.copySongArtworkFromGallery
import dev.citali.lunartune.utils.formatSongTags
import dev.citali.lunartune.utils.parseIdSet
import dev.citali.lunartune.utils.parsePlayerDesignStyleOverrides
import dev.citali.lunartune.utils.parseSongTags
import dev.citali.lunartune.utils.parseTabMap
import dev.citali.lunartune.utils.rememberEnumPreference
import dev.citali.lunartune.utils.rememberPreference
import dev.citali.lunartune.utils.setPlayerDesignStyleOverride
import dev.citali.lunartune.utils.setTabMapValue
import dev.citali.lunartune.utils.toggleIdSet

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PerSongSettingsDialog(
    songId: String,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val settingsPlayerDesignStyle by rememberEnumPreference(PlayerDesignStyleKey, defaultValue = PlayerDesignStyle.V4)
    val (playerStyleOverrides, onPlayerStyleOverridesChange) =
        rememberPreference(PlayerDesignStyleOverridesKey, "")
    val (albumArtOverrides, onAlbumArtOverridesChange) =
        rememberPreference(PerSongAlbumArtOverridesKey, "")
    val (tagsRaw, onTagsRawChange) = rememberPreference(PerSongTagsKey, "")
    val (neverRecommendRaw, onNeverRecommendChange) =
        rememberPreference(NeverRecommendSongIdsKey, "")

    val songStyle =
        remember(playerStyleOverrides, songId) {
            parsePlayerDesignStyleOverrides(playerStyleOverrides)[songId]
        }
    val hasCustomArt =
        remember(albumArtOverrides, songId) {
            !parseTabMap(albumArtOverrides)[songId].isNullOrBlank()
        }
    val currentTags =
        remember(tagsRaw, songId) {
            parseSongTags(parseTabMap(tagsRaw)[songId].orEmpty())
        }
    val neverRecommend =
        remember(neverRecommendRaw, songId) {
            songId in parseIdSet(neverRecommendRaw)
        }

    var showPlayerStyleDialog by rememberSaveable { mutableStateOf(false) }
    var showTagsDialog by rememberSaveable { mutableStateOf(false) }
    var tagsDraft by rememberSaveable { mutableStateOf("") }

    val galleryLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            val stored = copySongArtworkFromGallery(context, songId, uri)
            if (stored == null) {
                Toast.makeText(context, R.string.per_song_album_art_failed, Toast.LENGTH_SHORT).show()
            } else {
                onAlbumArtOverridesChange(setTabMapValue(albumArtOverrides, songId, stored))
                Toast.makeText(context, R.string.per_song_album_art_saved, Toast.LENGTH_SHORT).show()
            }
        }

    if (showPlayerStyleDialog) {
        ListDialog(onDismiss = { showPlayerStyleDialog = false }) {
            item {
                ListItem(
                    headlineContent = { Text(text = stringResource(R.string.player_style_use_default)) },
                    supportingContent = {
                        Text(
                            text = playerDesignStyleLabel(settingsPlayerDesignStyle),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    leadingContent = {
                        Icon(painter = painterResource(R.drawable.settings), contentDescription = null)
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                onPlayerStyleOverridesChange(
                                    setPlayerDesignStyleOverride(playerStyleOverrides, songId, null),
                                )
                                showPlayerStyleDialog = false
                            },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
            items(PlayerDesignStyle.entries) { style ->
                val selected = songStyle == style
                ListItem(
                    headlineContent = { Text(text = playerDesignStyleLabel(style)) },
                    leadingContent = {
                        Icon(
                            painter = painterResource(if (selected) R.drawable.done else R.drawable.style),
                            contentDescription = null,
                        )
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                onPlayerStyleOverridesChange(
                                    setPlayerDesignStyleOverride(playerStyleOverrides, songId, style),
                                )
                                showPlayerStyleDialog = false
                            },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
        }
    }

    if (showTagsDialog) {
        AlertDialog(
            onDismissRequest = { showTagsDialog = false },
            title = { Text(stringResource(R.string.per_song_custom_tags)) },
            text = {
                OutlinedTextField(
                    value = tagsDraft,
                    onValueChange = { tagsDraft = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.per_song_custom_tags_hint)) },
                    singleLine = false,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val formatted = formatSongTags(parseSongTags(tagsDraft))
                        onTagsRawChange(setTabMapValue(tagsRaw, songId, formatted.ifBlank { null }))
                        showTagsDialog = false
                    },
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showTagsDialog = false },
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.per_song_settings)) },
        text = {
            Column {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.player_style_for_song)) },
                    supportingContent = {
                        Text(
                            text = songStyle?.let { playerDesignStyleLabel(it) }
                                ?: stringResource(R.string.player_style_use_default),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    leadingContent = {
                        Icon(painter = painterResource(R.drawable.style), contentDescription = null)
                    },
                    modifier = Modifier.clickable { showPlayerStyleDialog = true },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.per_song_album_art)) },
                    supportingContent = {
                        Text(
                            text =
                                stringResource(
                                    if (hasCustomArt) {
                                        R.string.per_song_album_art_custom
                                    } else {
                                        R.string.per_song_album_art_default
                                    },
                                ),
                        )
                    },
                    leadingContent = {
                        Icon(painter = painterResource(R.drawable.image), contentDescription = null)
                    },
                    modifier = Modifier.clickable { galleryLauncher.launch("image/*") },
                    trailingContent =
                        if (hasCustomArt) {
                            {
                                TextButton(onClick = {
                                    onAlbumArtOverridesChange(setTabMapValue(albumArtOverrides, songId, null))
                                }) {
                                    Text(stringResource(R.string.reset))
                                }
                            }
                        } else {
                            null
                        },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.per_song_never_recommend)) },
                    supportingContent = {
                        Text(
                            text =
                                stringResource(
                                    if (neverRecommend) {
                                        R.string.per_song_never_recommend_on
                                    } else {
                                        R.string.per_song_never_recommend_off
                                    },
                                ),
                        )
                    },
                    leadingContent = {
                        Icon(painter = painterResource(R.drawable.block), contentDescription = null)
                    },
                    modifier =
                        Modifier.clickable {
                            onNeverRecommendChange(toggleIdSet(neverRecommendRaw, songId, !neverRecommend))
                        },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.per_song_custom_tags)) },
                    supportingContent = {
                        Text(
                            text =
                                if (currentTags.isEmpty()) {
                                    stringResource(R.string.per_song_custom_tags_none)
                                } else {
                                    formatSongTags(currentTags)
                                },
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    leadingContent = {
                        Icon(painter = painterResource(R.drawable.edit), contentDescription = null)
                    },
                    modifier =
                        Modifier.clickable {
                            tagsDraft = formatSongTags(currentTags)
                            showTagsDialog = true
                        },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) {
                Text(stringResource(android.R.string.ok))
            }
        },
    )
}

@Composable
fun playerDesignStyleLabel(style: PlayerDesignStyle): String =
    when (style) {
        PlayerDesignStyle.V1 -> stringResource(R.string.player_design_v1)
        PlayerDesignStyle.V2 -> stringResource(R.string.player_design_v2)
        PlayerDesignStyle.V3 -> stringResource(R.string.player_design_v3)
        PlayerDesignStyle.V4 -> stringResource(R.string.player_design_v4)
        PlayerDesignStyle.V5 -> stringResource(R.string.player_design_v5)
        PlayerDesignStyle.V6 -> stringResource(R.string.player_design_v6)
        PlayerDesignStyle.V7 -> stringResource(R.string.player_design_v7)
        PlayerDesignStyle.V7_LEGACY -> stringResource(R.string.player_design_v7_legacy)
        PlayerDesignStyle.V8 -> stringResource(R.string.player_design_v8)
        PlayerDesignStyle.V9 -> stringResource(R.string.player_design_v9)
        PlayerDesignStyle.V10 -> stringResource(R.string.player_design_v10)
    }
