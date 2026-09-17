package com.dulpick.app.feature.search

import androidx.lifecycle.viewModelScope
import com.dulpick.app.core.mvi.MviViewModel
import com.dulpick.app.domain.explore.ExploreError
import com.dulpick.app.domain.explore.ExploreRepository
import com.dulpick.app.domain.place.PlaceRepository
import com.dulpick.app.domain.search.RecentSearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val DEBOUNCE_MS = 300L

@HiltViewModel
@Suppress("TooGenericExceptionCaught")
class SearchViewModel @Inject constructor(
    private val exploreRepository: ExploreRepository,
    private val placeRepository: PlaceRepository,
    private val recentSearchRepository: RecentSearchRepository,
) : MviViewModel<SearchState, SearchIntent, SearchSideEffect>(SearchState()) {

    private var debounceJob: Job? = null
    private var searchJob: Job? = null
    private var loadMoreJob: Job? = null

    override fun onIntent(intent: SearchIntent) {
        when (intent) {
            SearchIntent.OnAppear -> loadRecent()
            is SearchIntent.QueryChanged -> onQueryChanged(intent.query)
            SearchIntent.SearchSubmitted -> onSubmit()
            is SearchIntent.RecentTapped -> onRecentTapped(intent.term)
            is SearchIntent.RecentDeleted -> updateRecent { recentSearchRepository.remove(intent.term) }
            SearchIntent.ClearRecent -> clearRecent()
            SearchIntent.ReachedEnd -> loadMore()
            is SearchIntent.TabSelected -> setState { copy(selectedTab = intent.tab) }
            is SearchIntent.ContentClicked -> postSideEffect(SearchSideEffect.ShowContentDetail(intent.id))
            is SearchIntent.PlaceClicked -> postSideEffect(SearchSideEffect.ShowPlaceDetail(intent.id))
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
        if (query.isEmpty()) {
            search(query)
            return
        }
        debounceJob = viewModelScope.launch {
            delay(DEBOUNCE_MS)
            search(query)
        }
    }

    private fun onSubmit() {
        val query = currentState.query.trim()
        if (query.isEmpty()) return
        debounceJob?.cancel()
        search(query)
        updateRecent { recentSearchRepository.add(query) }
    }

    private fun onRecentTapped(term: String) {
        val trimmed = term.trim()
        setState { copy(query = trimmed) }
        debounceJob?.cancel()
        if (trimmed.isEmpty()) return
        search(trimmed)
        updateRecent { recentSearchRepository.add(trimmed) }
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

    // 게시글·장소를 동시에 받는다
    private fun search(query: String) {
        searchJob?.cancel()
        loadMoreJob?.cancel()
        if (query.isEmpty()) {
            setState {
                copy(
                    contents = emptyList(), contentsPage = 0, contentsHasNext = true,
                    places = emptyList(), placesPage = 0, placesHasNext = true,
                    isSearching = false, isFirstSearch = true,
                )
            }
            return
        }
        setState {
            copy(isSearching = true, contentsPage = 0, contentsHasNext = true, placesPage = 0, placesHasNext = true)
        }
        searchJob = viewModelScope.launch {
            try {
                val contentsDeferred = async { exploreRepository.searchContents(query, 0, SearchState.PAGE_SIZE) }
                val placesDeferred = async { placeRepository.searchPlaces(query, 0, SearchState.PAGE_SIZE) }
                val contentPage = contentsDeferred.await()
                val placePage = placesDeferred.await()
                setState {
                    copy(
                        contents = contentPage.items, contentsHasNext = contentPage.hasNext, contentsPage = 1,
                        places = placePage.items, placesHasNext = placePage.hasNext, placesPage = 1,
                        isSearching = false, isFirstSearch = false,
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                setState { copy(isSearching = false, isFirstSearch = false) }
                if (error == ExploreError.Unauthorized) postSideEffect(SearchSideEffect.SessionExpired)
            }
        }
    }

    // 보고 있는 탭의 다음 페이지
    private fun loadMore() {
        when (currentState.selectedTab) {
            SearchTab.POST -> loadMoreContents()
            SearchTab.PLACE -> loadMorePlaces()
        }
    }

    private fun loadMoreContents() {
        val state = currentState
        if (state.query.isEmpty() || !state.contentsHasNext) return
        if (state.isLoadingMore || state.isSearching) return
        setState { copy(isLoadingMore = true) }
        val page = state.contentsPage
        val query = state.query
        loadMoreJob = viewModelScope.launch {
            try {
                val result = exploreRepository.searchContents(query, page, SearchState.PAGE_SIZE)
                setState {
                    copy(
                        contents = contents + result.items,
                        contentsHasNext = result.hasNext,
                        contentsPage = this.contentsPage + 1,
                        isLoadingMore = false,
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                setState { copy(isLoadingMore = false) }
                if (error == ExploreError.Unauthorized) postSideEffect(SearchSideEffect.SessionExpired)
            }
        }
    }

    private fun loadMorePlaces() {
        val state = currentState
        if (state.query.isEmpty() || !state.placesHasNext) return
        if (state.isLoadingMore || state.isSearching) return
        setState { copy(isLoadingMore = true) }
        val page = state.placesPage
        val query = state.query
        loadMoreJob = viewModelScope.launch {
            try {
                val result = placeRepository.searchPlaces(query, page, SearchState.PAGE_SIZE)
                setState {
                    copy(
                        places = places + result.items,
                        placesHasNext = result.hasNext,
                        placesPage = this.placesPage + 1,
                        isLoadingMore = false,
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                setState { copy(isLoadingMore = false) }
                if (error == ExploreError.Unauthorized) postSideEffect(SearchSideEffect.SessionExpired)
            }
        }
    }
}
