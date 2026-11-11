package com.dulpick.app.feature.course

import com.dulpick.app.domain.course.CourseError
import com.dulpick.app.domain.course.CourseRepository
import com.dulpick.app.domain.place.SavedPlace
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val DEBOUNCE_MS = 300L

// 「저장한 장소」 시트의 장소 검색. 뷰모델이 커지지 않게 여기로 뺀다
@Suppress("TooGenericExceptionCaught", "LongParameterList")
class CoursePlaceSearchController(
    private val repository: CourseRepository,
    private val scope: CoroutineScope,
    private val state: () -> CoursePlacePickState,
    private val update: (CoursePlacePickState.() -> CoursePlacePickState) -> Unit,
    private val effect: (CoursePlacePickSideEffect) -> Unit,
) {
    private var debounceJob: Job? = null
    private var searchJob: Job? = null
    private var moreJob: Job? = null

    fun onIntent(intent: CoursePlacePickIntent) {
        when (intent) {
            CoursePlacePickIntent.SearchChipClicked -> openField()
            is CoursePlacePickIntent.SearchQueryChanged -> changeQuery(intent.query)
            CoursePlacePickIntent.SearchReachedEnd -> loadMore()
            CoursePlacePickIntent.SearchFieldDismissed -> dismissField()
            CoursePlacePickIntent.SearchCancelClicked, CoursePlacePickIntent.SearchChipClearClicked -> clear()
            else -> Unit
        }
    }

    // 필터가 바뀌면 걸러진 뒤 보일 행이 없을 수 있다
    fun onFilterChanged() {
        loadMoreIfBlank()
    }

    // 목록이 뜨기 전에는 열지 않는다. 목록 실패 화면과 검색 화면이 섞이지 않게 한다
    private fun openField() {
        if (!state().isSearchAvailable) return
        update { copy(search = search.copy(isFieldOpen = true)) }
    }

    private fun changeQuery(query: String) {
        val previous = state().search.trimmedQuery
        update { copy(search = search.copy(query = query)) }
        // 공백만 늘었다. 돌던 요청을 끊지 않는다
        if (state().search.trimmedQuery == previous) return
        debounce()
    }

    // 빈 검색어로 키보드가 내려가면 「검색」 칩으로 돌아간다. 검색어가 있으면 검색어 칩이 된다
    private fun dismissField() {
        if (!state().search.isFieldOpen) return
        update { copy(search = search.copy(isFieldOpen = false)) }
        if (state().search.trimmedQuery.isEmpty()) clear()
    }

    // 검색을 푼다. 받아 둔 목록 전체로 돌아가고, 고른 장소와 찾은 장소 기록은 그대로 둔다
    private fun clear() {
        cancel()
        update { copy(search = search.copy(isFieldOpen = false, query = "").reset()) }
    }

    private fun cancel() {
        debounceJob?.cancel()
        searchJob?.cancel()
        moreJob?.cancel()
    }

    private fun debounce() {
        cancel()
        if (state().search.trimmedQuery.isEmpty()) {
            update { copy(search = search.reset()) }
            return
        }
        // 기다리는 동안 앞 검색어의 0건·실패 문구 대신 가짜 행을 보인다
        update {
            copy(search = search.copy(hasNext = false, isLoadingMore = false, load = CoursePlaceSearchLoad.IDLE))
        }
        debounceJob = scope.launch {
            delay(DEBOUNCE_MS)
            start()
        }
    }

    private fun start() {
        val query = state().search.trimmedQuery
        if (query.isEmpty()) return
        update {
            copy(
                search = search.copy(
                    page = 0,
                    hasNext = false,
                    isLoadingMore = false,
                    load = CoursePlaceSearchLoad.LOADING,
                ),
            )
        }
        searchJob = scope.launch {
            try {
                val page = repository.searchCoursePlaces(query, 0)
                update {
                    copy(
                        search = search
                            .copy(results = page.items, hasNext = page.hasNext, page = 0)
                            .remembering(page.items)
                            .copy(load = CoursePlaceSearchLoad.LOADED),
                    )
                }
                loadMoreIfBlank()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                update { copy(search = search.copy(results = emptyList(), load = CoursePlaceSearchLoad.FAILED)) }
                if (error == CourseError.Unauthorized) effect(CoursePlacePickSideEffect.SessionExpired)
            }
        }
    }

    private fun loadMore() {
        val search = state().search
        val query = search.trimmedQuery
        val canLoadMore = search.hasNext && !search.isLoadingMore && search.load == CoursePlaceSearchLoad.LOADED
        if (query.isEmpty() || !canLoadMore) return
        val next = search.page + 1
        update { copy(search = this.search.copy(isLoadingMore = true)) }
        moreJob = scope.launch {
            try {
                val page = repository.searchCoursePlaces(query, next)
                update {
                    copy(
                        search = this.search
                            .copy(
                                results = this.search.results + page.items,
                                hasNext = page.hasNext,
                                page = next,
                                isLoadingMore = false,
                            )
                            .remembering(page.items),
                    )
                }
                loadMoreIfBlank()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                failMore(error)
            }
        }
    }

    private fun failMore(error: Throwable) {
        update { copy(search = search.copy(isLoadingMore = false)) }
        if (error == CourseError.Unauthorized) {
            effect(CoursePlacePickSideEffect.SessionExpired)
            return
        }
        // 받은 행은 화면에 남긴다. hasNext 를 끄면 일시 실패가 영구 정지가 된다
        if (state().filteredPlaces.isEmpty()) {
            update { copy(search = search.copy(load = CoursePlaceSearchLoad.FAILED)) }
        }
    }

    // 앱이 걸러 보일 행이 없으면 끝에 닿을 행도 없다. 남은 페이지가 있으면 여기서 이어 부른다
    private fun loadMoreIfBlank() {
        val current = state()
        if (current.isSearching && current.filteredPlaces.isEmpty() && current.search.hasNext) loadMore()
    }
}

private fun CoursePlaceSearch.reset(): CoursePlaceSearch = copy(
    results = emptyList(),
    load = CoursePlaceSearchLoad.IDLE,
    page = 0,
    hasNext = false,
    isLoadingMore = false,
)

// 받은 검색 결과를 찾은 장소 기록에 더한다. 같은 장소는 한 번만 둔다
private fun CoursePlaceSearch.remembering(items: List<SavedPlace>): CoursePlaceSearch {
    val known = foundPlaces.map { it.id }.toSet()
    return copy(foundPlaces = foundPlaces + items.filter { it.id !in known })
}
