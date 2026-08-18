/*
 * LunarTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.citali.lunartune.ads.presentation

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import dev.citali.lunartune.ads.domain.OpenSupportPageUseCase
import dev.citali.lunartune.ads.domain.SupportPageOpenResult
import javax.inject.Inject

internal sealed interface SupportLunarTuneScreenState {
    @Immutable
    data object Loading : SupportLunarTuneScreenState

    @Immutable
    data object Success : SupportLunarTuneScreenState

    @Immutable
    data object Empty : SupportLunarTuneScreenState

    @Immutable
    data class Error(
        val reason: SupportLunarTuneError,
    ) : SupportLunarTuneScreenState
}

internal enum class SupportLunarTuneError {
    PageUnavailable,
}

internal enum class SupportLunarTuneUiEvent {
    OpenFailed,
}

@HiltViewModel
internal class SupportLunarTuneViewModel
    @Inject
    constructor(
        private val openSupportPage: OpenSupportPageUseCase,
    ) : ViewModel() {
        private val _screenState =
            MutableStateFlow<SupportLunarTuneScreenState>(SupportLunarTuneScreenState.Success)
        val screenState: StateFlow<SupportLunarTuneScreenState> = _screenState.asStateFlow()

        private val eventChannel = Channel<SupportLunarTuneUiEvent>(Channel.BUFFERED)
        val events = eventChannel.receiveAsFlow()

        fun onSupportLunarTuneClick() {
            if (_screenState.value is SupportLunarTuneScreenState.Loading) return
            _screenState.value = SupportLunarTuneScreenState.Loading
            when (openSupportPage()) {
                SupportPageOpenResult.Opened -> {
                    _screenState.value = SupportLunarTuneScreenState.Success
                }

                SupportPageOpenResult.Unavailable -> {
                    _screenState.value =
                        SupportLunarTuneScreenState.Error(SupportLunarTuneError.PageUnavailable)
                    eventChannel.trySend(SupportLunarTuneUiEvent.OpenFailed)
                }
            }
        }
    }
