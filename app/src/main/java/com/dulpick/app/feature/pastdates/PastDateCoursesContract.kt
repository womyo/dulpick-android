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
    // 첫 로딩이 오류로 끝났는지. 빈 목록과 구분해 재시도를 노출한다
    val hasError: Boolean = false,
    val isLoadingMore: Boolean = false,
    // 다음 페이지 로딩이 실패했는지. 실패 후 스크롤로 자동 재요청이 반복되지 않게 막는다
    val loadMoreFailed: Boolean = false,
    // 이번주 데이트 일정이 이미 있는지. 있으면 빈 상태에서 만들기 버튼을 숨긴다
    val hasCurrentCourse: Boolean = false,
) : UiState {
    // 다음 페이지를 받을 수 있는 상태(더 있고, 로딩 중 아니고, 직전 실패 없음)
    val canLoadMore: Boolean get() = hasNext && !isLoadingMore && !loadMoreFailed

    companion object {
        const val PAGE_SIZE = 20
    }
}

sealed interface PastDateCoursesIntent : UiIntent {
    data object ReachedEnd : PastDateCoursesIntent
    data object RetryClicked : PastDateCoursesIntent
    data object CreateCourseClicked : PastDateCoursesIntent
    data class CourseClicked(val id: String) : PastDateCoursesIntent
}

sealed interface PastDateCoursesSideEffect : UiSideEffect {
    data object OpenCreateCourse : PastDateCoursesSideEffect
    data class OpenCourseResult(val id: String) : PastDateCoursesSideEffect
}
