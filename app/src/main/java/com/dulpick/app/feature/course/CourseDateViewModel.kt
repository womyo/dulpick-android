package com.dulpick.app.feature.course

import androidx.lifecycle.viewModelScope
import com.dulpick.app.core.mvi.MviViewModel
import com.dulpick.app.domain.couple.CoupleRepository
import com.dulpick.app.domain.course.CourseError
import com.dulpick.app.domain.course.CourseRepository
import com.dulpick.app.domain.course.DateCourseTitle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
@Suppress("TooGenericExceptionCaught")
class CourseDateViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
    private val coupleRepository: CoupleRepository,
) : MviViewModel<CourseDateState, CourseDateIntent, CourseDateSideEffect>(initialState()) {

    private var createJob: Job? = null
    private var loadedPartner = false

    override fun onIntent(intent: CourseDateIntent) {
        when (intent) {
            CourseDateIntent.OnAppear -> loadPartner()
            CourseDateIntent.NextClicked -> createCourse()
            CourseDateIntent.BackClicked -> postSideEffect(CourseDateSideEffect.Dismissed)
            else -> onWheelIntent(intent)
        }
    }

    // 휠 시트 인텐트 (onIntent 복잡도 분리)
    private fun onWheelIntent(intent: CourseDateIntent) {
        when (intent) {
            CourseDateIntent.DateFieldClicked -> setState {
                copy(draftDate = date ?: draftDate, activeWheel = WheelTarget.DATE)
            }
            CourseDateIntent.TimeFieldClicked -> setState {
                copy(draftTime = time ?: draftTime, activeWheel = WheelTarget.TIME)
            }
            is CourseDateIntent.DraftDateChanged -> setState { copy(draftDate = intent.date) }
            is CourseDateIntent.DraftTimeChanged -> setState { copy(draftTime = intent.time) }
            CourseDateIntent.WheelConfirmed -> confirmWheel()
            CourseDateIntent.WheelDismissed -> setState { copy(activeWheel = null) }
            else -> Unit
        }
    }

    // 상대 닉네임은 제목 줄에만 쓴다. 조회에 실패하면 닉네임 없는 제목으로 둔다
    private fun loadPartner() {
        if (loadedPartner) return
        loadedPartner = true
        viewModelScope.launch {
            val status = try {
                coupleRepository.current()
            } catch (error: CancellationException) {
                throw error
            } catch (ignored: Throwable) {
                null
            }
            setState { copy(partnerNickname = status?.partner?.nickname?.takeIf { status.connected }) }
        }
    }

    private fun confirmWheel() {
        setState {
            when (activeWheel) {
                WheelTarget.DATE -> copy(date = draftDate, showsDateError = false, activeWheel = null)
                WheelTarget.TIME -> copy(time = draftTime, activeWheel = null)
                null -> this
            }
        }
    }

    // 날짜만 검증한다. 시간은 선택이다
    private fun createCourse() {
        val date = currentState.date ?: run {
            setState { copy(showsDateError = true) }
            return
        }
        if (createJob?.isActive == true) return
        setState { copy(showsDateError = false, isCreatingCourse = true) }
        val time = currentState.time
        createJob = viewModelScope.launch {
            try {
                val course = courseRepository.createCourse(DateCourseTitle.make(date), date, time)
                setState { copy(isCreatingCourse = false) }
                postSideEffect(CourseDateSideEffect.PlacePickRequested(course.id, course.version))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                setState { copy(isCreatingCourse = false) }
                if (error == CourseError.Unauthorized) {
                    postSideEffect(CourseDateSideEffect.SessionExpired)
                } else {
                    postSideEffect(CourseDateSideEffect.ShowToast("잠시 뒤 다시 시도해주세요"))
                }
            }
        }
    }

    private companion object {
        // 날짜 하한은 내일. 화면이 살아 있는 동안 자정을 넘겨도 바뀌지 않게 한 번만 센다
        fun initialState(): CourseDateState {
            val tomorrow = LocalDate.now().plusDays(1)
            return CourseDateState(
                draftDate = tomorrow,
                draftTime = LocalTime.now().withSecond(0).withNano(0),
                tomorrow = tomorrow,
            )
        }
    }
}
