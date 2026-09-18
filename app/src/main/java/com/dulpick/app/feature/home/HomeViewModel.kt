package com.dulpick.app.feature.home

import androidx.lifecycle.viewModelScope
import com.dulpick.app.core.mvi.MviViewModel
import com.dulpick.app.domain.explore.ContentSort
import com.dulpick.app.domain.explore.ExploreRepository
import com.dulpick.app.domain.home.HomeError
import com.dulpick.app.domain.home.HomeRepository
import com.dulpick.app.domain.profile.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@Suppress("TooGenericExceptionCaught")
class HomeViewModel @Inject constructor(
    private val homeRepository: HomeRepository,
    private val exploreRepository: ExploreRepository,
    private val profileRepository: ProfileRepository,
) : MviViewModel<HomeState, HomeIntent, HomeSideEffect>(HomeState()) {

    private var loadJob: Job? = null

    override fun onIntent(intent: HomeIntent) {
        when (intent) {
            // 홈이 보일 때마다 다시 받는다(탭 재진입·연결/해제 후 복귀 포함). iOS onAppear 는 가드가 없다
            HomeIntent.OnAppear -> load()
            HomeIntent.RefreshRequested -> load()
            else -> handleNavigation(intent)
        }
    }

    // 화면 이동으로 위임하는 탭 인텐트들
    private fun handleNavigation(intent: HomeIntent) {
        when (intent) {
            HomeIntent.CalendarClicked -> handleCalendar()
            HomeIntent.ConnectFlowRequested -> requestConnect()
            HomeIntent.CourseFlowRequested -> postSideEffect(HomeSideEffect.OpenCourseFlow)
            HomeIntent.BannerClicked -> openUpcomingCourse()
            is HomeIntent.RecommendationClicked -> postSideEffect(HomeSideEffect.OpenContentDetail(intent.id))
            is HomeIntent.PastScheduleClicked -> postSideEffect(HomeSideEffect.OpenPastSchedule(intent.id))
            is HomeIntent.SavedPlaceClicked -> postSideEffect(HomeSideEffect.OpenPlaceDetail(intent.id))
            HomeIntent.SavedPlacesSeeAllClicked -> postSideEffect(HomeSideEffect.OpenSavedPlacesAll)
            else -> Unit
        }
    }

    // 연결됐으면 지난 데이트, 아니면 커플 연결로 (iOS calendarTapped 대응)
    private fun handleCalendar() {
        if (currentState.isConnected) {
            postSideEffect(HomeSideEffect.OpenPastDates(currentState.upcomingSchedule != null))
        } else {
            postSideEffect(HomeSideEffect.OpenConnectFlow)
        }
    }

    // 배너는 미연결일 때만 보이지만 방어적으로 연결 상태는 무시한다
    private fun requestConnect() {
        if (!currentState.isConnected) postSideEffect(HomeSideEffect.OpenConnectFlow)
    }

    private fun openUpcomingCourse() {
        currentState.upcomingSchedule?.let { postSideEffect(HomeSideEffect.OpenUpcomingCourse(it.id)) }
    }

    // 요약·저장장소·추천을 동시에 받는다. didLoad 를 false 로 되돌리지 않아
    // 재진입 갱신은 스켈레톤 없이 조용히 바뀐다(첫 진입만 초기값 false 라 스켈레톤). 하나가 실패해도 각자 완료로 친다
    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            launch { loadHome() }
            launch { loadSavedPlaces() }
            launch { loadRecommendations() }
        }
    }

    private suspend fun loadHome() {
        try {
            val summary = homeRepository.home()
            setState {
                copy(
                    didLoadSummary = true,
                    connected = summary.connected,
                    nickname = summary.myNickname,
                    partnerName = summary.partnerNickname,
                    upcomingSchedule = summary.currentDateCourse,
                )
            }
            // 지난 데이트는 연결됐을 때만 있다
            if (summary.connected) loadPastDates()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            // 실패해도 스켈레톤은 걷는다. 인증 만료만 상위로 올려 로그인으로 보낸다
            setState { copy(didLoadSummary = true) }
            if (error == HomeError.Unauthorized) postSideEffect(HomeSideEffect.SessionExpired)
        }
    }

    private suspend fun loadSavedPlaces() {
        try {
            val places = homeRepository.recentSavedPlaces(HomeState.RECENT_SAVED_PLACE_COUNT)
            setState { copy(didLoadSaved = true, savedPlaces = places) }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            // 실패 시 기존 데이터는 그대로 두고, 완료만 알려 스켈레톤을 걷는다
            setState { copy(didLoadSaved = true) }
        }
    }

    private suspend fun loadRecommendations() {
        // datePreference 를 등록한 사용자만 성향(PREFERENCE) 정렬, 아니면 인기(POPULAR)
        val sort = try {
            if (profileRepository.profile().datePreference != null) ContentSort.PREFERENCE else ContentSort.POPULAR
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            ContentSort.POPULAR
        }
        try {
            val page = exploreRepository.contents(0, HomeState.RECOMMENDATION_COUNT, sort)
            setState { copy(didLoadRecommendations = true, recommendations = page.items) }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            setState { copy(didLoadRecommendations = true) }
        }
    }

    private suspend fun loadPastDates() {
        try {
            val dates = homeRepository.pastDates(HomeState.PAST_DATE_COUNT)
            setState { copy(pastSchedules = dates) }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            // 실패 시 기존 목록은 그대로 둔다(재진입 갱신 실패로 빈 화면 깜빡임 방지). loadSavedPlaces 와 동일
        }
    }
}
