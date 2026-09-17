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

    // 요약·저장장소·추천을 동시에 받는다. 하나가 실패해도 각자 스켈레톤만 걷는다
    private fun load() {
        loadJob?.cancel()
        setState { copy(didLoadSummary = false, didLoadSaved = false, didLoadRecommendations = false) }
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
            // 지난 데이트 실패는 섹션만 비운다
        }
    }
}
