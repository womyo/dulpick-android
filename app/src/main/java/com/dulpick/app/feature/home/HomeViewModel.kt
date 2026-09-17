package com.dulpick.app.feature.home

import androidx.lifecycle.viewModelScope
import com.dulpick.app.core.mvi.MviViewModel
import com.dulpick.app.domain.course.DateCourseSummary
import com.dulpick.app.domain.explore.Content
import com.dulpick.app.domain.home.DateSchedule
import com.dulpick.app.domain.place.Place
import com.dulpick.app.domain.place.PlaceCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

// TODO: 홈 API(요약·추천·저장장소) 연동 전까지 목데이터로 UI 만 확인한다
private const val MOCK_LOAD_DELAY_MS = 600L

@HiltViewModel
class HomeViewModel @Inject constructor() :
    MviViewModel<HomeState, HomeIntent, HomeSideEffect>(HomeState()) {

    init {
        load()
    }

    override fun onIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.OnAppear -> Unit
            HomeIntent.RefreshRequested -> load()
            else -> handleNavigation(intent)
        }
    }

    // 화면 이동으로 위임하는 탭 인텐트들
    private fun handleNavigation(intent: HomeIntent) {
        when (intent) {
            HomeIntent.CalendarClicked -> postSideEffect(HomeSideEffect.OpenCalendar)
            HomeIntent.ConnectFlowRequested -> postSideEffect(HomeSideEffect.OpenConnectFlow)
            HomeIntent.CourseFlowRequested -> postSideEffect(HomeSideEffect.OpenCourseFlow)
            HomeIntent.BannerClicked ->
                currentState.upcomingSchedule?.let { postSideEffect(HomeSideEffect.OpenUpcomingCourse(it.id)) }
            is HomeIntent.RecommendationClicked -> postSideEffect(HomeSideEffect.OpenContentDetail(intent.id))
            is HomeIntent.PastScheduleClicked -> postSideEffect(HomeSideEffect.OpenPastSchedule(intent.id))
            is HomeIntent.SavedPlaceClicked -> postSideEffect(HomeSideEffect.OpenPlaceDetail(intent.id))
            HomeIntent.SavedPlacesSeeAllClicked -> postSideEffect(HomeSideEffect.OpenSavedPlacesAll)
            else -> Unit
        }
    }

    // 첫 진입·새로고침 모두 같은 데이터를 다시 읽는다. 스켈레톤을 잠깐 보여준 뒤 채운다
    private fun load() {
        setState {
            copy(didLoadSummary = false, didLoadSaved = false, didLoadRecommendations = false)
        }
        viewModelScope.launch {
            delay(MOCK_LOAD_DELAY_MS)
            setState {
                copy(
                    nickname = MOCK_NICKNAME,
                    partnerName = MOCK_PARTNER_NAME,
                    upcomingSchedule = MOCK_UPCOMING,
                    recommendations = MOCK_RECOMMENDATIONS,
                    pastSchedules = MOCK_PAST_SCHEDULES,
                    savedPlaces = MOCK_SAVED_PLACES,
                    isRefreshing = false,
                    didLoadSummary = true,
                    didLoadSaved = true,
                    didLoadRecommendations = true,
                )
            }
        }
    }

    private companion object {
        const val MOCK_NICKNAME = "둘픽"
        const val MOCK_PARTNER_NAME = "연인"

        // 예정 일정이 없으면 코스 배너가 뜬다. 예정 배너를 보려면 값을 넣는다
        val MOCK_UPCOMING: DateCourseSummary? = null

        val MOCK_RECOMMENDATIONS = List(6) { index ->
            Content(
                id = "rec-$index",
                title = "성수동 데이트하기 좋은 장소 모음 ${index + 1}",
                placeCount = index + 3,
                thumbnailUrls = emptyList(),
            )
        }

        val MOCK_PAST_SCHEDULES = listOf(
            DateSchedule(id = "past-1", title = "홍대 나들이", placeCount = 4, date = "2026.08.10"),
            DateSchedule(id = "past-2", title = "성수 카페 투어", placeCount = 3, date = "2026.07.28"),
            DateSchedule(id = "past-3", title = "한강 피크닉", placeCount = 2, date = "2026.07.15"),
        )

        val MOCK_SAVED_PLACES = listOf(
            Place("p1", "블루보틀 성수", PlaceCategory.CAFE, 12, emptyList()),
            Place("p2", "롯데월드타워", PlaceCategory.TOURISM, 34, emptyList()),
            Place("p3", "성수 소품샵", PlaceCategory.SHOPPING, 8, emptyList()),
            Place("p4", "이태원 맛집", PlaceCategory.FOOD, 21, emptyList()),
            Place("p5", "잠실 볼링장", PlaceCategory.ACTIVITY, 5, emptyList()),
        )
    }
}
