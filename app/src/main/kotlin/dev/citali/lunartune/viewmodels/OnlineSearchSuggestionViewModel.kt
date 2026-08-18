/*
 * LunarTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.citali.lunartune.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import dev.citali.lunartune.aicontentfilter.FilterAiContentUseCase
import dev.citali.lunartune.aicontentfilter.LoadAiContentFilterPolicyUseCase
import dev.citali.lunartune.constants.HideExplicitKey
import dev.citali.lunartune.constants.HideVideoKey
import dev.citali.lunartune.db.MusicDatabase
import dev.citali.lunartune.db.entities.SearchHistory
import dev.citali.lunartune.innertube.YouTube
import dev.citali.lunartune.innertube.models.YTItem
import dev.citali.lunartune.innertube.models.filterExplicit
import dev.citali.lunartune.innertube.models.filterVideo
import dev.citali.lunartune.utils.dataStore
import dev.citali.lunartune.utils.get
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class OnlineSearchSuggestionViewModel
    @Inject
    constructor(
        @ApplicationContext val context: Context,
        private val database: MusicDatabase,
        private val loadAiContentFilterPolicy: LoadAiContentFilterPolicyUseCase,
        private val filterAiContent: FilterAiContentUseCase,
    ) : ViewModel() {
        private val query = MutableStateFlow("")
        private val _viewState = MutableStateFlow(SearchSuggestionViewState())
        val viewState = _viewState.asStateFlow()

        init {
            viewModelScope.launch {
                query
                    .flatMapLatest { query ->
                        if (query.isEmpty()) {
                            database.searchHistory().map { history ->
                                SearchSuggestionViewState(
                                    history = history,
                                )
                            }
                        } else {
                            val result = YouTube.searchSuggestions(query).getOrNull()
                            val aiContentFilterPolicy = loadAiContentFilterPolicy()
                            database
                                .searchHistory(query)
                                .map { it.take(3) }
                                .map { history ->
                                    SearchSuggestionViewState(
                                        history = history,
                                        suggestions =
                                            result
                                                ?.queries
                                                ?.filter { query ->
                                                    history.none { it.query == query }
                                                }.orEmpty(),
                                        items =
                                            filterAiContent(
                                                result
                                                    ?.recommendedItems
                                                    ?.filterExplicit(
                                                        context.dataStore.get(
                                                            HideExplicitKey,
                                                            false,
                                                        ),
                                                    )?.filterVideo(context.dataStore.get(HideVideoKey, false))
                                                    .orEmpty(),
                                                aiContentFilterPolicy,
                                            ),
                                    )
                                }
                        }
                    }.collect {
                        _viewState.value = it
                    }
            }
        }

        fun updateQuery(query: String) {
            this.query.value = query
        }

        fun deleteHistory(history: SearchHistory) {
            database.query {
                delete(history)
            }
        }
    }

data class SearchSuggestionViewState(
    val history: List<SearchHistory> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val items: List<YTItem> = emptyList(),
)
