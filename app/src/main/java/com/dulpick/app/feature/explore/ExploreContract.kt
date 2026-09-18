package com.dulpick.app.feature.explore

import com.dulpick.app.core.mvi.UiIntent
import com.dulpick.app.core.mvi.UiSideEffect
import com.dulpick.app.core.mvi.UiState
import com.dulpick.app.domain.explore.Content

data class ExploreState(
    val contents: List<Content> = emptyList(),
    val page: Int = 0,
    val hasNext: Boolean = true,
    val isLoadingContents: Boolean = false,
    val filters: List<String> = listOf(POPULAR_FILTER),
    val selectedFilter: String = POPULAR_FILTER,
) : UiState {

    // 인기면 기본 제목, 태그를 고르면 그 태그를 제목으로
    val sectionTitle: String
        get() = if (selectedFilter == POPULAR_FILTER) "지금 인기있는 장소" else selectedFilter

    // 목록이 비었는데 로딩 중인 최초 진입·필터 전환 구간
    val isInitialLoading: Boolean
        get() = contents.isEmpty() && isLoadingContents

    companion object {
        // 항상 맨 앞에 두는 기본 필터. 뒤로 서버 인기 태그가 붙는다
        const val POPULAR_FILTER = "인기"
        const val PAGE_SIZE = 10
    }
}

sealed interface ExploreIntent : UiIntent {
    data object OnAppear : ExploreIntent
    data object ReachedEnd : ExploreIntent
    data class FilterTapped(val filter: String) : ExploreIntent
    data object SearchClicked : ExploreIntent
    data class ContentClicked(val id: String) : ExploreIntent
}

sealed interface ExploreSideEffect : UiSideEffect {
    data object SearchRequested : ExploreSideEffect
    data class ShowContentDetail(val id: String) : ExploreSideEffect
    data object SessionExpired : ExploreSideEffect
}
