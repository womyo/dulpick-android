package com.dulpick.app.feature.explore

import androidx.lifecycle.viewModelScope
import com.dulpick.app.core.mvi.MviViewModel
import com.dulpick.app.domain.explore.ContentPage
import com.dulpick.app.domain.explore.ExploreError
import com.dulpick.app.domain.explore.ExploreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@Suppress("TooGenericExceptionCaught")
class ExploreViewModel @Inject constructor(
    private val exploreRepository: ExploreRepository,
) : MviViewModel<ExploreState, ExploreIntent, ExploreSideEffect>(ExploreState()) {

    // 칩을 바꾸면 진행 중이던 이전 로드를 취소해 결과가 섞이지 않게 한다
    private var loadJob: Job? = null

    override fun onIntent(intent: ExploreIntent) {
        when (intent) {
            // 최초 진입에만 첫 페이지 로드, 탭 재진입 시엔 유지된 목록 그대로 둔다
            ExploreIntent.OnAppear -> if (currentState.contents.isEmpty()) loadNext()
            ExploreIntent.ReachedEnd -> loadNext()
            is ExploreIntent.FilterTapped -> filterTapped(intent.filter)
            ExploreIntent.SearchClicked -> postSideEffect(ExploreSideEffect.SearchRequested)
            is ExploreIntent.ContentClicked -> postSideEffect(ExploreSideEffect.ShowContentDetail(intent.id))
        }
    }

    // 칩을 바꾸면 그리드를 리셋하고 첫 페이지부터 다시 받는다
    private fun filterTapped(filter: String) {
        if (filter == currentState.selectedFilter) return
        setState {
            copy(
                selectedFilter = filter,
                contents = emptyList(),
                page = 0,
                hasNext = true,
                isLoadingContents = false,
            )
        }
        loadNext()
    }

    // 로딩 중이거나 마지막 페이지면 무시. 인기면 목록, 태그면 검색을 받는다
    private fun loadNext() {
        val state = currentState
        if (state.isLoadingContents || !state.hasNext) return
        setState { copy(isLoadingContents = true) }
        val requestedPage = state.page
        val selected = state.selectedFilter
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                val result = if (selected == ExploreState.POPULAR_FILTER) {
                    exploreRepository.contents(requestedPage, ExploreState.PAGE_SIZE)
                } else {
                    exploreRepository.searchContents(tagQuery(selected), requestedPage, ExploreState.PAGE_SIZE)
                }
                applyResult(result, requestedPage)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                // page 는 그대로 둬 다음 스크롤에서 같은 페이지 재시도
                setState { copy(isLoadingContents = false) }
                if (error == ExploreError.Unauthorized) postSideEffect(ExploreSideEffect.SessionExpired)
            }
        }
    }

    private fun applyResult(page: ContentPage, requestedPage: Int) {
        setState {
            // 인기 태그는 첫 페이지 응답에 담겨 온다. 서버값으로 칩을 구성한다
            val nextFilters = if (requestedPage == 0 && page.popularTags.isNotEmpty()) {
                listOf(ExploreState.POPULAR_FILTER) + page.popularTags.map { "#$it" }
            } else {
                filters
            }
            copy(
                isLoadingContents = false,
                contents = contents + page.items,
                hasNext = page.hasNext,
                page = this.page + 1,
                filters = nextFilters,
            )
        }
    }

    // "#성수" → "성수". 검색어에는 해시 기호를 빼고 넘긴다
    private fun tagQuery(filter: String): String = filter.removePrefix("#")
}
