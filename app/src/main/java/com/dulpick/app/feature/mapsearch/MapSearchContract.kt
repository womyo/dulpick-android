package com.dulpick.app.feature.mapsearch

import com.dulpick.app.core.mvi.UiIntent
import com.dulpick.app.core.mvi.UiSideEffect
import com.dulpick.app.core.mvi.UiState
import com.dulpick.app.domain.place.PlaceSearchResult
import com.dulpick.app.domain.place.SavedPlace

data class MapSearchState(
    val query: String = "",
    val recentSearches: List<String> = emptyList(),
    val results: List<PlaceSearchResult> = emptyList(),
    val page: Int = 0,
    val hasNext: Boolean = true,
    val isSearching: Boolean = false,
    val isLoadingMore: Boolean = false,
    // 한 번이라도 검색해 로드가 끝났는지(빈 결과 문구를 로딩과 구분)
    val hasSearched: Boolean = false,
    val isFailed: Boolean = false,
    // 켜면 저장한 장소만 찾는다. 커플 연결 전에는 내가 저장한 장소다
    val isSavedOnly: Boolean = false,
) : UiState {
    // 검색어가 비면 최근 검색어를 보여준다
    val showRecent: Boolean get() = query.trim().isEmpty()

    // 다 불러온 뒤 결과가 없는 상태
    val isEmptyResult: Boolean
        get() = !showRecent && loaded && results.isEmpty() && !hasNext

    // 받은 페이지가 걸러져 비었지만 다음 페이지가 남은 상태. 화면은 로딩을 보인다
    val isAwaitingResults: Boolean
        get() = !showRecent && loaded && results.isEmpty() && hasNext

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
    data class SavedOnlyChanged(val isOn: Boolean) : MapSearchIntent
}

sealed interface MapSearchSideEffect : UiSideEffect {
    // 제출: 전체 결과를 지도로
    data class SearchConfirmed(
        val query: String,
        val results: List<PlaceSearchResult>,
        val isSavedOnly: Boolean,
    ) : MapSearchSideEffect

    // 행 탭: 고른 장소 하나를 지도에 올리고 상세로
    data class PlaceSelected(val query: String, val result: PlaceSearchResult) : MapSearchSideEffect

    // 「저장한 곳만 보기」 결과 행. 카카오 번호가 없어 저장 목록처럼 장소 번호로 상세를 연다
    data class SavedPlaceSelected(val savedPlace: SavedPlace) : MapSearchSideEffect

    data object SessionExpired : MapSearchSideEffect
}
