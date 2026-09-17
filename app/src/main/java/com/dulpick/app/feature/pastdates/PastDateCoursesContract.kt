package com.dulpick.app.feature.pastdates

import com.dulpick.app.core.mvi.UiIntent
import com.dulpick.app.core.mvi.UiSideEffect
import com.dulpick.app.core.mvi.UiState
import com.dulpick.app.domain.home.DateSchedule

data class PastDateCoursesState(
    val courses: List<DateSchedule> = emptyList(),
    val totalCount: Int = 0,
    val page: Int = 0,
    val hasNext: Boolean = true,
    val hasLoaded: Boolean = false,
    val isLoadingMore: Boolean = false,
    // 이번주 데이트 일정이 이미 있는지. 있으면 빈 상태에서 만들기 버튼을 숨긴다
    val hasCurrentCourse: Boolean = false,
) : UiState {
    companion object {
        const val PAGE_SIZE = 20
    }
}

sealed interface PastDateCoursesIntent : UiIntent {
    data object ReachedEnd : PastDateCoursesIntent
    data object CreateCourseClicked : PastDateCoursesIntent
    data class CourseClicked(val id: String) : PastDateCoursesIntent
}

sealed interface PastDateCoursesSideEffect : UiSideEffect {
    data object OpenCreateCourse : PastDateCoursesSideEffect
    data class OpenCourseResult(val id: String) : PastDateCoursesSideEffect
}
