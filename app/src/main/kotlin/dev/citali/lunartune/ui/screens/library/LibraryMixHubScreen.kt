/*
 * LunarTune (2026)
 * © cognitiveshadows03 — github.com/cognitiveshadows03
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.citali.lunartune.ui.screens.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import dev.citali.lunartune.LocalPlayerAwareWindowInsets
import dev.citali.lunartune.R
import dev.citali.lunartune.ui.component.IconButton
import dev.citali.lunartune.ui.utils.backToMain

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryMixHubScreen(navController: NavController) {
    val playerAwareBottomPadding =
        LocalPlayerAwareWindowInsets.current
            .only(WindowInsetsSides.Bottom)
            .asPaddingValues()
            .calculateBottomPadding()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.library_mix_hub)) },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = null,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding()),
            contentPadding =
                PaddingValues(
                    start = 24.dp,
                    end = 24.dp,
                    top = 8.dp,
                    bottom = playerAwareBottomPadding + 24.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.library_mix_hub_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        ShortcutCard(
                            title = stringResource(R.string.smart_playlist_on_repeat),
                            countText = stringResource(R.string.smart_playlist_on_repeat_desc),
                            iconRes = R.drawable.replay,
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                            iconColor = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.weight(1f),
                            onClick = { navController.navigate("auto_playlist/on_repeat") },
                        )
                        ShortcutCard(
                            title = stringResource(R.string.smart_playlist_forgotten),
                            countText = stringResource(R.string.smart_playlist_forgotten_desc),
                            iconRes = R.drawable.history,
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
                            iconColor = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f),
                            onClick = { navController.navigate("auto_playlist/forgotten") },
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        ShortcutCard(
                            title = stringResource(R.string.smart_playlist_recent),
                            countText = stringResource(R.string.smart_playlist_recent_desc),
                            iconRes = R.drawable.history,
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                            iconColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                            onClick = { navController.navigate("auto_playlist/recent") },
                        )
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
