package com.dulpick.app.feature.search

import com.dulpick.app.core.mvi.UiIntent
import com.dulpick.app.core.mvi.UiSideEffect
import com.dulpick.app.core.mvi.UiState
import com.dulpick.app.domain.explore.Content
import com.dulpick.app.domain.place.Place

enum class SearchTab(val title: String) {
    POST("게시글"),
    PLACE("장소"),
}

data class SearchState(
    val query: String = "",
    val recentSearches: List<String> = emptyList(),
    val selectedTab: SearchTab = SearchTab.POST,
    val contents: List<Content> = emptyList(),
    val contentsPage: Int = 0,
    val contentsHasNext: Boolean = true,
    val places: List<Place> = emptyList(),
    val placesPage: Int = 0,
    val placesHasNext: Boolean = true,
    val isSearching: Boolean = false,
    val isLoadingMore: Boolean = false,
    // 로딩 표시는 첫 검색에만. 재검색 때는 직전 화면을 유지해 빈 상태가 깜빡이지 않게 한다
    val isFirstSearch: Boolean = true,
) : UiState {
    val showRecent: Boolean get() = query.isEmpty()

    // 게시글/장소 중 하나라도 결과가 있으면 탭을 노출한다
    val hasSearchResult: Boolean get() = contents.isNotEmpty() || places.isNotEmpty()

    // 선택된 탭 기준 결과 유무
    val hasResult: Boolean
        get() = when (selectedTab) {
            SearchTab.POST -> contents.isNotEmpty()
            SearchTab.PLACE -> places.isNotEmpty()
        }

    companion object {
        const val PAGE_SIZE = 10
    }
}

sealed interface SearchIntent : UiIntent {
    data object OnAppear : SearchIntent
    data class QueryChanged(val query: String) : SearchIntent
    data object SearchSubmitted : SearchIntent
    data class RecentTapped(val term: String) : SearchIntent
    data class RecentDeleted(val term: String) : SearchIntent
    data object ClearRecent : SearchIntent
    data object ReachedEnd : SearchIntent
    data class TabSelected(val tab: SearchTab) : SearchIntent
    data class ContentClicked(val id: String) : SearchIntent
    data class PlaceClicked(val id: String) : SearchIntent
}

sealed interface SearchSideEffect : UiSideEffect {
    // TODO: 게시물/장소 상세는 지도 단계에서
    data class ShowContentDetail(val id: String) : SearchSideEffect
    data class ShowPlaceDetail(val id: String) : SearchSideEffect
    data object SessionExpired : SearchSideEffect
}
