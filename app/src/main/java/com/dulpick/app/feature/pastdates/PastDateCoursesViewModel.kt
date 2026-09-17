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
            PastDateCoursesIntent.CreateCourseClicked ->
                postSideEffect(PastDateCoursesSideEffect.OpenCreateCourse)
            is PastDateCoursesIntent.CourseClicked ->
                postSideEffect(PastDateCoursesSideEffect.OpenCourseResult(intent.id))
        }
    }

    private fun loadFirst() {
        viewModelScope.launch {
            val page = runCatching { homeRepository.pastCourses(0, PastDateCoursesState.PAGE_SIZE) }.getOrNull()
            setState {
                copy(
                    hasLoaded = true,
                    courses = page?.courses ?: emptyList(),
                    totalCount = page?.totalCount ?: 0,
                    hasNext = page?.hasNext ?: false,
                    page = 1,
                )
            }
        }
    }

    private fun loadMore() {
        val state = currentState
        if (!state.hasLoaded || !state.hasNext || state.isLoadingMore) return
        setState { copy(isLoadingMore = true) }
        val requestedPage = state.page
        viewModelScope.launch {
            try {
                val page = homeRepository.pastCourses(requestedPage, PastDateCoursesState.PAGE_SIZE)
                setState {
                    copy(
                        courses = courses + page.courses,
                        totalCount = page.totalCount,
                        hasNext = page.hasNext,
                        page = this.page + 1,
                        isLoadingMore = false,
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                setState { copy(isLoadingMore = false) }
            }
        }
    }
}
