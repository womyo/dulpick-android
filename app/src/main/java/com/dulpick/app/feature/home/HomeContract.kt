package com.dulpick.app.feature.home

import com.dulpick.app.core.mvi.UiIntent
import com.dulpick.app.core.mvi.UiSideEffect
import com.dulpick.app.core.mvi.UiState
import com.dulpick.app.domain.course.DateCourseSummary
import com.dulpick.app.domain.explore.Content
import com.dulpick.app.domain.home.DateSchedule
import com.dulpick.app.domain.place.Place

data class HomeState(
    // 커플 연결 여부. 서버가 connected=true 인데 partnerNickname=null 인 경우도 있어 별도로 둔다
    val connected: Boolean = false,
    val nickname: String = "",
    val partnerName: String? = null,
    val upcomingSchedule: DateCourseSummary? = null,
    val recommendations: List<Content> = emptyList(),
    val pastSchedules: List<DateSchedule> = emptyList(),
    val savedPlaces: List<Place> = emptyList(),
    // 당겨서 새로고침 중인지
    val isRefreshing: Boolean = false,
    // 요약(홈 API) 로드 완료. 헤더·배너를 이 이후 실제로 그린다
    val didLoadSummary: Boolean = false,
    // 최근 저장 장소 로드 완료(성공·실패 모두). 로딩과 빈 상태를 구분한다
    val didLoadSaved: Boolean = false,
    // 추천 로드 완료(성공·실패 모두). 실패해도 스켈레톤을 걷는다
    val didLoadRecommendations: Boolean = false,
) : UiState {

    val isConnected: Boolean get() = connected

    val showsPastSchedules: Boolean get() = isConnected && pastSchedules.isNotEmpty()

    val visiblePastSchedules: List<DateSchedule> get() = pastSchedules.take(PAST_DATE_COUNT)

    val visibleSavedPlaces: List<Place> get() = savedPlaces.take(RECENT_SAVED_PLACE_COUNT)

    companion object {
        const val RECOMMENDATION_COUNT = 10
        const val RECENT_SAVED_PLACE_COUNT = 5
        const val PAST_DATE_COUNT = 3
    }
}

sealed interface HomeIntent : UiIntent {
    data object OnAppear : HomeIntent
    data object RefreshRequested : HomeIntent
    data object CalendarClicked : HomeIntent
    data object ConnectFlowRequested : HomeIntent
    data object CourseFlowRequested : HomeIntent
    data object BannerClicked : HomeIntent
    data class RecommendationClicked(val id: String) : HomeIntent
    data class PastScheduleClicked(val id: String) : HomeIntent
    data class SavedPlaceClicked(val id: String) : HomeIntent
    data object SavedPlacesSeeAllClicked : HomeIntent
}

sealed interface HomeSideEffect : UiSideEffect {
    // 연결됐을 때 헤더 달력 → 지난 데이트 코스 (hasCurrentCourse 로 빈 상태 버튼 노출 여부 결정)
    data class OpenPastDates(val hasCurrentCourse: Boolean) : HomeSideEffect
    data object OpenConnectFlow : HomeSideEffect
    data object OpenCourseFlow : HomeSideEffect
    data class OpenUpcomingCourse(val id: String) : HomeSideEffect
    data class OpenContentDetail(val id: String) : HomeSideEffect
    data class OpenPastSchedule(val id: String) : HomeSideEffect
    data class OpenPlaceDetail(val id: String) : HomeSideEffect
    data object OpenSavedPlacesAll : HomeSideEffect
    data object SessionExpired : HomeSideEffect
}
