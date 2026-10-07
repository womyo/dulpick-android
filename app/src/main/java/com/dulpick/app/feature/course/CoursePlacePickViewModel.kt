package com.dulpick.app.feature.course

import androidx.lifecycle.viewModelScope
import com.dulpick.app.core.mvi.MviViewModel
import com.dulpick.app.domain.couple.CoupleRepository
import com.dulpick.app.domain.course.CourseError
import com.dulpick.app.domain.course.CourseRepository
import com.dulpick.app.domain.course.DateCourse
import com.dulpick.app.domain.course.DateCourseContent
import com.dulpick.app.domain.place.PlaceOwnership
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@Suppress("TooGenericExceptionCaught")
class CoursePlacePickViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
    private val coupleRepository: CoupleRepository,
) : MviViewModel<CoursePlacePickState, CoursePlacePickIntent, CoursePlacePickSideEffect>(
    CoursePlacePickState(),
) {
    private var dateCourseId: String? = null
    private var started = false

    // 앞 화면이 만든 코스. 제목·날짜·시간을 그대로 다시 올려야 해서 들고 있는다.
    // 낙관적 락 번호(version)도 여기서 본다
    private var course: DateCourse? = null

    override fun onIntent(intent: CoursePlacePickIntent) {
        when (intent) {
            is CoursePlacePickIntent.Start -> start(intent)
            CoursePlacePickIntent.RetryClicked -> load()
            is CoursePlacePickIntent.OwnershipSelected ->
                setState { copy(selectedOwnership = intent.ownership) }
            is CoursePlacePickIntent.CategorySelected ->
                setState { copy(selectedCategory = intent.category) }
            is CoursePlacePickIntent.PlaceToggled -> toggle(intent.id)
            // 더하기로 열렸으면 코스를 저장하지 않고 고른 것만 돌려준다
            CoursePlacePickIntent.BuildClicked ->
                if (currentState.mode == CoursePlacePickMode.ADD) returnPicked() else saveCourse()
            CoursePlacePickIntent.ConflictDismissed -> setState { copy(conflictMessage = null) }
            CoursePlacePickIntent.BackClicked -> postSideEffect(CoursePlacePickSideEffect.Dismissed)
        }
    }

    private fun start(intent: CoursePlacePickIntent.Start) {
        if (started) return
        started = true
        dateCourseId = intent.dateCourseId
        setState { copy(mode = intent.mode, excludedPlaceIds = intent.excluding) }
        load()
        loadCouple()
    }

    // 담을 수 있는 장소를 읽는다. 코스를 처음 짤 때는 제목·날짜·시간·version 도 함께 읽는다
    private fun load() {
        setState { copy(load = CoursePlaceLoad.LOADING) }
        viewModelScope.launch {
            try {
                course = dateCourseId?.let { courseRepository.course(it) }
                val places = courseRepository.coursePlaces()
                setState { copy(places = places, load = CoursePlaceLoad.LOADED) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                setState { copy(load = CoursePlaceLoad.FAILED) }
                if (error == CourseError.Unauthorized) {
                    postSideEffect(CoursePlacePickSideEffect.SessionExpired)
                }
            }
        }
    }

    // 조회에 실패하면 연결 안 됨과 같게 저장자 필터를 숨긴다
    private fun loadCouple() {
        viewModelScope.launch {
            val status = try {
                coupleRepository.current()
            } catch (error: CancellationException) {
                throw error
            } catch (ignored: Throwable) {
                null
            }
            val connected = status?.connected == true
            setState {
                copy(
                    isCoupleConnected = connected,
                    // 연동이 풀린 채로 저장자 필터가 남아 있으면 목록이 이유 없이 좁아진다
                    selectedOwnership = if (connected) selectedOwnership else PlaceOwnership.TOGETHER,
                )
            }
        }
    }

    // 고른 순서가 곧 번호다. 다시 누르면 빼고 뒤 번호가 당겨진다
    private fun toggle(id: String) {
        setState {
            copy(
                selectedPlaceIds = if (id in selectedPlaceIds) {
                    selectedPlaceIds - id
                } else {
                    selectedPlaceIds + id
                },
            )
        }
    }

    // 고른 장소를 그 순서대로 돌려준다
    private fun returnPicked() {
        val picked = currentState.selectedPlaceIds.mapNotNull { id ->
            currentState.places.firstOrNull { it.id == id }
        }
        if (picked.isNotEmpty()) postSideEffect(CoursePlacePickSideEffect.PlacesPicked(picked))
    }

    private fun saveCourse() {
        val id = dateCourseId ?: return
        val saved = course ?: return
        val placeIds = currentState.selectedPlaceIds
        if (placeIds.isEmpty() || currentState.isSavingCourse) return
        setState { copy(isSavingCourse = true) }
        val content = DateCourseContent(
            title = saved.title,
            date = saved.scheduledDate,
            time = saved.scheduledTime,
            placeIds = placeIds,
        )
        viewModelScope.launch { runSave(id, content, saved.version) }
    }

    private suspend fun runSave(id: String, content: DateCourseContent, version: Int) {
        try {
            val updated = courseRepository.updateCourse(id, content, version)
            // 낡은 값으로 다시 저장하지 않게 한다
            course = updated
            setState { copy(isSavingCourse = false) }
            postSideEffect(CoursePlacePickSideEffect.BuildRequested(updated.id))
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            handleSaveFailure(id, error)
        }
    }

    private suspend fun handleSaveFailure(id: String, error: Throwable) {
        when (error) {
            CourseError.Unauthorized -> {
                setState { copy(isSavingCourse = false) }
                postSideEffect(CoursePlacePickSideEffect.SessionExpired)
            }
            // 그냥 재시도하지 않는다. 최신 코스를 읽고 사용자에게 알린다
            CourseError.Conflict -> reloadAfterConflict(id)
            else -> {
                setState { copy(isSavingCourse = false) }
                postSideEffect(CoursePlacePickSideEffect.ShowToast("잠시 뒤 다시 시도해주세요"))
            }
        }
    }

    private suspend fun reloadAfterConflict(id: String) {
        try {
            course = courseRepository.course(id)
            setState {
                copy(
                    isSavingCourse = false,
                    conflictMessage = "상대방이 코스를 먼저 바꿨어요. 다시 확인해주세요",
                )
            }
        } catch (error: CancellationException) {
            throw error
        } catch (ignored: Throwable) {
            setState { copy(isSavingCourse = false) }
            postSideEffect(CoursePlacePickSideEffect.ShowToast("잠시 뒤 다시 시도해주세요"))
        }
    }
}
