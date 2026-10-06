package com.dulpick.app.feature.course

import androidx.lifecycle.viewModelScope
import com.dulpick.app.core.mvi.MviViewModel
import com.dulpick.app.domain.couple.CoupleRepository
import com.dulpick.app.domain.course.CourseError
import com.dulpick.app.domain.course.CourseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@Suppress("TooGenericExceptionCaught")
class CourseResultViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
    private val coupleRepository: CoupleRepository,
) : MviViewModel<CourseResultState, CourseResultIntent, CourseResultSideEffect>(CourseResultState()) {

    private var dateCourseId: String? = null

    override fun onIntent(intent: CourseResultIntent) {
        when (intent) {
            is CourseResultIntent.Start -> start(intent.dateCourseId, intent.origin)
            CourseResultIntent.RetryClicked -> load()
            CourseResultIntent.ConflictReloadRequested -> {
                setState { copy(course = null) }
                postSideEffect(CourseResultSideEffect.ShowToast("상대방이 먼저 바꿔서 최신 코스를 불러왔어요"))
                load()
            }
            CourseResultIntent.NotifyClicked -> notifyPartner()
            CourseResultIntent.EditClicked -> dateCourseId?.let {
                postSideEffect(CourseResultSideEffect.EditRequested(it))
            }
            CourseResultIntent.BackClicked -> postSideEffect(CourseResultSideEffect.Dismissed)
        }
    }

    private fun start(id: String, origin: CourseResultOrigin) {
        if (dateCourseId == id) return
        dateCourseId = id
        setState { copy(origin = origin) }
        load()
        loadPartner()
    }

    private fun load() {
        val id = dateCourseId ?: return
        setState { copy(load = CourseResultLoad.LOADING) }
        viewModelScope.launch {
            try {
                val course = courseRepository.course(id)
                setState { copy(course = course, load = CourseResultLoad.LOADED) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                setState { copy(load = CourseResultLoad.FAILED) }
                if (error == CourseError.Unauthorized) {
                    postSideEffect(CourseResultSideEffect.SessionExpired)
                }
            }
        }
    }

    // 상대 닉네임은 알리기 버튼 글자에만 쓴다. 실패하면 "상대에게" 로 둔다
    private fun loadPartner() {
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

    private fun notifyPartner() {
        val id = dateCourseId ?: return
        if (currentState.isNotifyingPartner) return
        setState { copy(isNotifyingPartner = true) }
        viewModelScope.launch {
            try {
                courseRepository.notifyPartner(id)
                setState { copy(isNotifyingPartner = false) }
                postSideEffect(CourseResultSideEffect.ShowToast("상대에게 코스를 알렸어요"))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                setState { copy(isNotifyingPartner = false) }
                if (error == CourseError.Unauthorized) {
                    postSideEffect(CourseResultSideEffect.SessionExpired)
                } else {
                    postSideEffect(CourseResultSideEffect.ShowToast("잠시 뒤 다시 시도해주세요"))
                }
            }
        }
    }
}
