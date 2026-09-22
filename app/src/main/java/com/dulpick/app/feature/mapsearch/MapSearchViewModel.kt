package com.dulpick.app.feature.mapsearch

import androidx.lifecycle.viewModelScope
import com.dulpick.app.core.mvi.MviViewModel
import com.dulpick.app.di.MapRecentSearch
import com.dulpick.app.domain.explore.ExploreError
import com.dulpick.app.domain.place.PlaceRepository
import com.dulpick.app.domain.search.RecentSearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val DEBOUNCE_MS = 300L

// 지도 전용 장소 검색 입력 화면. 장소만 검색하고, 제출/행탭 결과를 지도로 올린다 (iOS PlaceSearchFeature 대응)
@HiltViewModel
@Suppress("TooGenericExceptionCaught")
class MapSearchViewModel @Inject constructor(
    private val placeRepository: PlaceRepository,
    @MapRecentSearch private val recentSearchRepository: RecentSearchRepository,
) : MviViewModel<MapSearchState, MapSearchIntent, MapSearchSideEffect>(MapSearchState()) {

    private var debounceJob: Job? = null
    private var searchJob: Job? = null
    private var loadMoreJob: Job? = null

    override fun onIntent(intent: MapSearchIntent) {
        when (intent) {
            MapSearchIntent.OnAppear -> loadRecent()
            is MapSearchIntent.QueryChanged -> onQueryChanged(intent.query)
            MapSearchIntent.SearchSubmitted -> onSubmit()
            is MapSearchIntent.RecentTapped -> onRecentTapped(intent.term)
            is MapSearchIntent.RecentDeleted -> updateRecent { recentSearchRepository.remove(intent.term) }
            MapSearchIntent.ClearRecent -> clearRecent()
            MapSearchIntent.ReachedEnd -> loadMore()
            is MapSearchIntent.PlaceClicked -> onPlaceClicked(intent.id)
        }
    }

    private fun loadRecent() {
        viewModelScope.launch {
            val recent = recentSearchRepository.recent()
            setState { copy(recentSearches = recent) }
        }
    }

    private fun onQueryChanged(query: String) {
        setState { copy(query = query) }
        debounceJob?.cancel()
        val normalized = query.trim()
        if (normalized.isEmpty()) {
            search("")
            return
        }
        debounceJob = viewModelScope.launch {
            delay(DEBOUNCE_MS)
            search(normalized)
        }
    }

    // 제출: 결과가 있으면 최근검색을 저장하고 전체 결과를 지도로 올린다
    private fun onSubmit() {
        val query = currentState.query.trim()
        if (query.isEmpty() || currentState.results.isEmpty()) return
        updateRecent { recentSearchRepository.add(query) }
        postSideEffect(MapSearchSideEffect.SearchConfirmed(query, currentState.results))
    }

    private fun onRecentTapped(term: String) {
        val trimmed = term.trim()
        setState { copy(query = trimmed) }
        debounceJob?.cancel()
        if (trimmed.isEmpty()) return
        search(trimmed)
        updateRecent { recentSearchRepository.add(trimmed) }
    }

    // 행 탭: 최근검색을 저장하고 고른 장소 하나를 지도로 올린다
    private fun onPlaceClicked(id: String) {
        val place = currentState.results.firstOrNull { it.id == id } ?: return
        val query = currentState.query.trim()
        if (query.isNotEmpty()) updateRecent { recentSearchRepository.add(query) }
        postSideEffect(MapSearchSideEffect.PlaceSelected(query, place))
    }

    private fun clearRecent() {
        viewModelScope.launch {
            recentSearchRepository.clear()
            setState { copy(recentSearches = emptyList()) }
        }
    }

    private fun updateRecent(block: suspend () -> List<String>) {
        viewModelScope.launch {
            val updated = block()
            setState { copy(recentSearches = updated) }
        }
    }

    private fun search(query: String) {
        searchJob?.cancel()
        loadMoreJob?.cancel()
        if (query.isEmpty()) {
            setState {
                copy(results = emptyList(), page = 0, hasNext = true, isSearching = false, hasSearched = false)
            }
            return
        }
        setState { copy(isSearching = true, page = 0, hasNext = true) }
        searchJob = viewModelScope.launch {
            try {
                val result = placeRepository.searchPlaces(query, 0, MapSearchState.PAGE_SIZE)
                setState {
                    copy(
                        results = result.items,
                        hasNext = result.hasNext,
                        page = 1,
                        isSearching = false,
                        hasSearched = true,
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                setState { copy(isSearching = false, hasSearched = true) }
                if (error == ExploreError.Unauthorized) postSideEffect(MapSearchSideEffect.SessionExpired)
            }
        }
    }

    private fun loadMore() {
        val state = currentState
        val query = state.query.trim()
        val canLoadMore = state.hasNext && !state.isLoadingMore && !state.isSearching
        if (query.isEmpty() || !canLoadMore) return
        setState { copy(isLoadingMore = true) }
        val page = state.page
        loadMoreJob = viewModelScope.launch {
            try {
                val result = placeRepository.searchPlaces(query, page, MapSearchState.PAGE_SIZE)
                setState {
                    copy(
                        // 페이지 간 id 중복은 LazyColumn 중복 key 크래시를 부르므로 제거한다
                        results = (results + result.items).distinctBy { it.id },
                        hasNext = result.hasNext,
                        page = this.page + 1,
                        isLoadingMore = false,
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                setState { copy(isLoadingMore = false) }
                if (error == ExploreError.Unauthorized) postSideEffect(MapSearchSideEffect.SessionExpired)
            }
        }
    }
}
