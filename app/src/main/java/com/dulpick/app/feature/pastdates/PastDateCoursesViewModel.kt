package com.dulpick.app.feature.pastdates

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.dulpick.app.core.mvi.MviViewModel
import com.dulpick.app.domain.home.HomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import javax.inject.Inject

const val ARG_PASTDATES_HAS_CURRENT = "hasCurrentCourse"

@HiltViewModel
@Suppress("TooGenericExceptionCaught")
class PastDateCoursesViewModel @Inject constructor(
    private val homeRepository: HomeRepository,
    savedStateHandle: SavedStateHandle,
) : MviViewModel<PastDateCoursesState, PastDateCoursesIntent, PastDateCoursesSideEffect>(
    PastDateCoursesState(hasCurrentCourse = savedStateHandle[ARG_PASTDATES_HAS_CURRENT] ?: false),
) {

    init {
        loadFirst()
    }

    override fun onIntent(intent: PastDateCoursesIntent) {
        when (intent) {
            PastDateCoursesIntent.ReachedEnd -> loadMore()
            PastDateCoursesIntent.RetryClicked -> loadFirst()
            PastDateCoursesIntent.CreateCourseClicked ->
                postSideEffect(PastDateCoursesSideEffect.OpenCreateCourse)
            is PastDateCoursesIntent.CourseClicked ->
                postSideEffect(PastDateCoursesSideEffect.OpenCourseResult(intent.id))
        }
    }

    private fun loadFirst() {
        setState { copy(hasError = false) }
        viewModelScope.launch {
            val page = try {
                homeRepository.pastCourses(0, PastDateCoursesState.PAGE_SIZE)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                // 통신 오류를 빈 목록으로 위장하지 않는다. 오류 상태로 두어 재시도할 수 있게 한다
                setState { copy(hasLoaded = true, hasError = true) }
                return@launch
            }
            setState {
                copy(
                    hasLoaded = true,
                    hasError = false,
                    courses = page.courses,
                    totalCount = page.totalCount,
                    hasNext = page.hasNext,
                    page = 1,
                )
            }
        }
    }

    private fun loadMore() {
        val state = currentState
        if (!state.hasLoaded || !state.canLoadMore) return
        setState { copy(isLoadingMore = true) }
        val requestedPage = state.page
        viewModelScope.launch {
            try {
                val page = homeRepository.pastCourses(requestedPage, PastDateCoursesState.PAGE_SIZE)
                setState {
                    copy(
                        // 페이지 간 id 중복은 LazyColumn 중복 key 크래시를 부르므로 제거한다
                        courses = (courses + page.courses).distinctBy { it.id },
                        totalCount = page.totalCount,
                        hasNext = page.hasNext,
                        page = this.page + 1,
                        isLoadingMore = false,
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                // 실패를 기록해 마지막 항목 재노출로 자동 재요청이 반복되지 않게 한다
                setState { copy(isLoadingMore = false, loadMoreFailed = true) }
            }
        }
    }
}
