package com.dulpick.app.feature.mapsearch

import com.dulpick.app.core.mvi.UiIntent
import com.dulpick.app.core.mvi.UiSideEffect
import com.dulpick.app.core.mvi.UiState
import com.dulpick.app.domain.place.Place

data class MapSearchState(
    val query: String = "",
    val recentSearches: List<String> = emptyList(),
    val results: List<Place> = emptyList(),
    val page: Int = 0,
    val hasNext: Boolean = true,
    val isSearching: Boolean = false,
    val isLoadingMore: Boolean = false,
    // 한 번이라도 검색해 로드가 끝났는지(빈 결과 문구를 로딩과 구분)
    val hasSearched: Boolean = false,
) : UiState {
    // 검색어가 비면 최근 검색어를 보여준다
    val showRecent: Boolean get() = query.trim().isEmpty()

    // 다 불러온 뒤 결과가 없는 상태
    val isEmptyResult: Boolean
        get() = !showRecent && loaded && results.isEmpty()

    // 검색이 끝난(로딩 아님) 상태
    private val loaded: Boolean get() = hasSearched && !isSearching

    companion object {
        const val PAGE_SIZE = 20
    }
}

sealed interface MapSearchIntent : UiIntent {
    data object OnAppear : MapSearchIntent
    data class QueryChanged(val query: String) : MapSearchIntent
    data object SearchSubmitted : MapSearchIntent
    data class RecentTapped(val term: String) : MapSearchIntent
    data class RecentDeleted(val term: String) : MapSearchIntent
    data object ClearRecent : MapSearchIntent
    data object ReachedEnd : MapSearchIntent
    data class PlaceClicked(val id: String) : MapSearchIntent
}

sealed interface MapSearchSideEffect : UiSideEffect {
    // 제출: 전체 결과를 지도로 (검색 결과 모드) (iOS searchConfirmed 대응)
    data class SearchConfirmed(val query: String, val places: List<Place>) : MapSearchSideEffect

    // 행 탭: 고른 장소 하나를 지도에 올리고 상세로 (iOS placeSelected 대응)
    data class PlaceSelected(val query: String, val place: Place) : MapSearchSideEffect

    data object SessionExpired : MapSearchSideEffect
}
