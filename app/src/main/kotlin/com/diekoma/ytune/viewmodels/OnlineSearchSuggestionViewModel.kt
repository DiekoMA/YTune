/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.diekoma.ytune.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diekoma.ytune.innertube.YouTube
import com.diekoma.ytune.innertube.models.YTItem
import com.diekoma.ytune.innertube.models.filterExplicit
import com.diekoma.ytune.innertube.models.filterVideo
import com.diekoma.ytune.constants.HideExplicitKey
import com.diekoma.ytune.constants.HideVideoKey
import com.diekoma.ytune.db.MusicDatabase
import com.diekoma.ytune.db.entities.SearchHistory
import com.diekoma.ytune.innertube.models.SongItem
import com.diekoma.ytune.utils.dataStore
import com.diekoma.ytune.utils.get
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class OnlineSearchSuggestionViewModel
@Inject
constructor(
    @ApplicationContext val context: Context,
    private val database: MusicDatabase,
) : ViewModel() {
    val query = MutableStateFlow("")
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
                                    items = result
                                        ?.recommendedItems
                                        ?.filterExplicit(
                                            context.dataStore.get(
                                                HideExplicitKey,
                                                false,
                                            ),
                                        )
                                        ?.filterVideo(context.dataStore.get(HideVideoKey, false)).orEmpty(),
                                )
                            }
                    }
                }.collect {
                    _viewState.value = it
                }
        }
    }

    fun saveSearchHistory(item: SongItem) {
        viewModelScope.launch {
            database.query {
                upsert(
                    SearchHistory(
                        query = item.title,
                        videoId = item.id,
                        title = item.title,
                        subtitle = item.artists.joinToString { it.name },
                        thumbnailUrl = item.thumbnail,
                    )
                )
            }
        }
    }
}

data class SearchSuggestionViewState(
    val history: List<SearchHistory> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val items: List<YTItem> = emptyList(),
)
