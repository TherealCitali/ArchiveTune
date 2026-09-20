package dev.citali.lunartune.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
internal fun LyricsSourceLabel(
    source: String?,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    val sourceName =
        when (source) {
            null, "", "REMOTE" -> "a remote provider"
            "EMBEDDED" -> "embedded song metadata"
            "USER_SELECTION" -> "your selected provider"
            "USER_EDIT" -> "your edit"
            "AI_TRANSLATION" -> "AI translation"
            else -> source
        }
    Text(
        text = "Lyrics fetched from $sourceName",
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
        style = MaterialTheme.typography.labelMedium,
        color = color.copy(alpha = 0.68f),
        textAlign = TextAlign.Center,
        maxLines = 1,
    )
}
