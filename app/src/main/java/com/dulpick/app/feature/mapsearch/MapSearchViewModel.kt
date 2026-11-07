package com.dulpick.app.feature.mapsearch

import androidx.lifecycle.viewModelScope
import com.dulpick.app.core.mvi.MviViewModel
import com.dulpick.app.di.MapRecentSearch
import com.dulpick.app.domain.common.Page
import com.dulpick.app.domain.course.CourseError
import com.dulpick.app.domain.course.CourseRepository
import com.dulpick.app.domain.explore.ExploreError
import com.dulpick.app.domain.place.PlaceRepository
import com.dulpick.app.domain.place.PlaceSearchResult
import com.dulpick.app.domain.search.RecentSearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val DEBOUNCE_MS = 300L

// 지도 전용 장소 검색 입력 화면. 장소만 검색하고, 제출·행탭 결과를 지도로 올린다
@HiltViewModel
@Suppress("TooGenericExceptionCaught", "TooManyFunctions")
class MapSearchViewModel @Inject constructor(
    private val placeRepository: PlaceRepository,
    private val courseRepository: CourseRepository,
    @MapRecentSearch private val recentSearchRepository: RecentSearchRepository,
) : MviViewModel<MapSearchState, MapSearchIntent, MapSearchSideEffect>(MapSearchState()) {

    private var debounceJob: Job? = null
    private var searchJob: Job? = null
    private var loadMoreJob: Job? = null

    override fun onIntent(intent: MapSearchIntent) {
        when (intent) {
            MapSearchIntent.OnAppear -> onAppear()
            is MapSearchIntent.QueryChanged -> onQueryChanged(intent.query)
            MapSearchIntent.SearchSubmitted -> onSubmit()
            is MapSearchIntent.RecentTapped -> onRecentTapped(intent.term)
            is MapSearchIntent.RecentDeleted -> updateRecent { recentSearchRepository.remove(intent.term) }
            MapSearchIntent.ClearRecent -> clearRecent()
            MapSearchIntent.ReachedEnd -> loadMore()
            is MapSearchIntent.PlaceClicked -> onPlaceClicked(intent.id)
            is MapSearchIntent.SavedOnlyChanged -> onSavedOnlyChanged(intent.isOn)
        }
    }

    private fun onAppear() {
        viewModelScope.launch {
            val recent = recentSearchRepository.recent()
            setState { copy(recentSearches = recent) }
        }
    }

    // 토글을 바꾸면 기다리지 않고 지금 검색어로 다시 찾는다
    private fun onSavedOnlyChanged(isOn: Boolean) {
        setState { copy(isSavedOnly = isOn) }
        val query = currentState.query.trim()
        if (query.isEmpty()) return
        debounceJob?.cancel()
        search(query)
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
        val state = currentState
        val query = state.query.trim()
        // 로딩 중엔 이전 토글의 결과가 남아 있다. 그대로 올리면 토글 상태와 결과가 어긋난다
        if (query.isEmpty() || state.isSearching || state.results.isEmpty()) return
        updateRecent { recentSearchRepository.add(query) }
        postSideEffect(MapSearchSideEffect.SearchConfirmed(query, state.results, state.isSavedOnly))
    }

    private fun onRecentTapped(term: String) {
        val trimmed = term.trim()
        setState { copy(query = trimmed) }
        debounceJob?.cancel()
        if (trimmed.isEmpty()) return
        search(trimmed)
        updateRecent { recentSearchRepository.add(trimmed) }
    }

    // 행 탭: 최근검색을 저장하고 고른 장소 하나를 지도로 올린다.
    // 토글 결과는 카카오 번호가 없어 저장 목록처럼 장소 번호로 연다
    private fun onPlaceClicked(id: String) {
        val result = currentState.results.firstOrNull { it.id == id } ?: return
        val query = currentState.query.trim()
        if (query.isNotEmpty()) updateRecent { recentSearchRepository.add(query) }
        val savedPlace = result.toSavedPlace()
        if (currentState.isSavedOnly && savedPlace != null) {
            postSideEffect(MapSearchSideEffect.SavedPlaceSelected(savedPlace))
        } else {
            postSideEffect(MapSearchSideEffect.PlaceSelected(query, result))
        }
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
                copy(
                    results = emptyList(), page = 0, hasNext = true,
                    isSearching = false, hasSearched = false, isFailed = false,
                )
            }
            return
        }
        setState { copy(isSearching = true, page = 0, hasNext = true, isFailed = false) }
        searchJob = viewModelScope.launch {
            val isSavedOnly = currentState.isSavedOnly
            try {
                val result = fetchPage(query, 0, isSavedOnly)
                setState {
                    copy(
                        results = result.items,
                        hasNext = result.hasNext,
                        page = 1,
                        isSearching = false,
                        hasSearched = true,
                    )
                }
                loadMoreIfBlank()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                setState { copy(results = emptyList(), isSearching = false, hasSearched = true, isFailed = true) }
                if (isUnauthorized(error)) postSideEffect(MapSearchSideEffect.SessionExpired)
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
        val isSavedOnly = state.isSavedOnly
        loadMoreJob = viewModelScope.launch {
            try {
                val result = fetchPage(query, page, isSavedOnly)
                setState {
                    copy(
                        // 페이지 간 id 중복은 LazyColumn 중복 key 크래시를 부르므로 제거한다
                        results = (results + result.items).distinctBy { it.id },
                        hasNext = result.hasNext,
                        page = this.page + 1,
                        isLoadingMore = false,
                    )
                }
                loadMoreIfBlank()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                // 받아 둔 페이지는 화면에 남긴다. hasNext 를 끄면 일시 실패가 영구 정지가 된다
                setState { copy(isLoadingMore = false) }
                if (currentState.results.isNotEmpty()) return@launch
                setState { copy(isFailed = true) }
                if (isUnauthorized(error)) postSideEffect(MapSearchSideEffect.SessionExpired)
            }
        }
    }

    // 좌표가 없어 걸러진 페이지는 행이 없어 끝에 닿을 일도 없다. 남은 페이지가 있으면 여기서 이어 부른다
    private fun loadMoreIfBlank() {
        if (currentState.results.isEmpty() && currentState.hasNext) loadMore()
    }

    // 토글이 켜져 있으면 커플 저장 장소 검색을, 꺼져 있으면 지도 검색을 부른다
    private suspend fun fetchPage(query: String, page: Int, isSavedOnly: Boolean): Page<PlaceSearchResult> {
        if (!isSavedOnly) return placeRepository.searchPlaces(query, page, MapSearchState.PAGE_SIZE)
        val saved = courseRepository.searchCoursePlaces(query, page)
        return Page(items = saved.items.map(PlaceSearchResult::of), hasNext = saved.hasNext)
    }

    private fun isUnauthorized(error: Throwable): Boolean =
        error == ExploreError.Unauthorized || error == CourseError.Unauthorized
}
