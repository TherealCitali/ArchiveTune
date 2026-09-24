/*
 * LunarTune (2026)
 * © cognitiveshadows03 — github.com/cognitiveshadows03
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.citali.lunartune.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.navigation.NavController
import dev.citali.lunartune.LocalPlayerAwareWindowInsets
import dev.citali.lunartune.R
import dev.citali.lunartune.ui.component.EnumListPreference
import dev.citali.lunartune.ui.component.IconButton
import dev.citali.lunartune.ui.component.PreferenceGroup
import dev.citali.lunartune.ui.component.SwitchPreference
import dev.citali.lunartune.ui.utils.backToMain
import dev.citali.lunartune.utils.rememberEnumPreference
import dev.citali.lunartune.utils.rememberPreference

val MotionPressKey = booleanPreferencesKey("motionPress")
val MotionSheetsKey = booleanPreferencesKey("motionSheets")
val MotionMiniKey = booleanPreferencesKey("motionMini")
enum class TabTransitionStyle {
    FADE,
    BLOOM,
    SPRING_SLIDE,
}

val TabTransitionKey = stringPreferencesKey("tabTransition")
val MotionBarKey = booleanPreferencesKey("motionBar")
val MotionListsKey = booleanPreferencesKey("motionLists")
val MotionMicroKey = booleanPreferencesKey("motionMicro")

@Composable
fun MotionSettings(navController: NavController) {
    val (motionPress, onMotionPressChange) = rememberPreference(MotionPressKey, defaultValue = false)
    val (motionSheets, onMotionSheetsChange) = rememberPreference(MotionSheetsKey, defaultValue = false)
    val (motionMini, onMotionMiniChange) = rememberPreference(MotionMiniKey, defaultValue = false)
    val (tabTransition, onTabTransitionChange) =
        rememberEnumPreference(TabTransitionKey, defaultValue = TabTransitionStyle.BLOOM)
    val (motionBar, onMotionBarChange) = rememberPreference(MotionBarKey, defaultValue = false)
    val (motionLists, onMotionListsChange) = rememberPreference(MotionListsKey, defaultValue = false)
    val (motionMicro, onMotionMicroChange) = rememberPreference(MotionMicroKey, defaultValue = false)

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.motion_settings_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain,
                    ) {
                        Icon(
                            painterResource(R.drawable.arrow_back),
                            contentDescription = null,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            Modifier
                .padding(innerPadding)
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(
                        WindowInsetsSides.Horizontal,
                    ),
                ).windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(
                        WindowInsetsSides.Bottom,
                    ),
                ).verticalScroll(rememberScrollState())
                .padding(bottom = SettingsDimensions.ScreenBottomPadding),
        ) {
            PreferenceGroup(title = stringResource(R.string.motion_settings_title)) {
                item {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.motion_press)) },
                        description = stringResource(R.string.motion_press_desc),
                        icon = { Icon(painterResource(R.drawable.animation), null) },
                        checked = motionPress,
                        onCheckedChange = onMotionPressChange,
                    )
                }

                item {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.motion_sheets)) },
                        description = stringResource(R.string.motion_sheets_desc),
                        icon = { Icon(painterResource(R.drawable.animation), null) },
                        checked = motionSheets,
                        onCheckedChange = onMotionSheetsChange,
                    )
                }

                item {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.motion_mini)) },
                        description = stringResource(R.string.motion_mini_desc),
                        icon = { Icon(painterResource(R.drawable.animation), null) },
                        checked = motionMini,
                        onCheckedChange = onMotionMiniChange,
                    )
                }

                item {
                    EnumListPreference(
                        title = { Text(stringResource(R.string.motion_nav)) },
                        description = stringResource(R.string.motion_nav_desc),
                        icon = { Icon(painterResource(R.drawable.animation), null) },
                        selectedValue = tabTransition,
                        onValueSelected = onTabTransitionChange,
                        valueText = {
                            when (it) {
                                TabTransitionStyle.FADE -> stringResource(R.string.tab_transition_fade)
                                TabTransitionStyle.BLOOM -> stringResource(R.string.tab_transition_bloom)
                                TabTransitionStyle.SPRING_SLIDE -> stringResource(R.string.tab_transition_spring)
                            }
                        },
                    )
                }

                item {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.motion_bar)) },
                        description = stringResource(R.string.motion_bar_desc),
                        icon = { Icon(painterResource(R.drawable.animation), null) },
                        checked = motionBar,
                        onCheckedChange = onMotionBarChange,
                    )
                }

                item {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.motion_lists)) },
                        description = stringResource(R.string.motion_lists_desc),
                        icon = { Icon(painterResource(R.drawable.animation), null) },
                        checked = motionLists,
                        onCheckedChange = onMotionListsChange,
                    )
                }

                item {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.motion_micro)) },
                        description = stringResource(R.string.motion_micro_desc),
                        icon = { Icon(painterResource(R.drawable.animation), null) },
                        checked = motionMicro,
                        onCheckedChange = onMotionMicroChange,
                    )
                }
            }
        }
    }
}
