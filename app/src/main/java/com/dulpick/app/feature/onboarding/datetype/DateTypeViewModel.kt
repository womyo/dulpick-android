package com.dulpick.app.feature.onboarding.datetype

import androidx.lifecycle.viewModelScope
import com.dulpick.app.core.mvi.MviViewModel
import com.dulpick.app.domain.profile.ProfileError
import com.dulpick.app.domain.profile.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DateTypeViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
) : MviViewModel<DateTypeState, DateTypeIntent, DateTypeSideEffect>(DateTypeState()) {

    override fun onIntent(intent: DateTypeIntent) {
        when (intent) {
            is DateTypeIntent.IndoorOutdoorSelected -> select { copy(indoorOutdoor = intent.value) }
            is DateTypeIntent.ActivityLevelSelected -> select { copy(activityLevel = intent.value) }
            is DateTypeIntent.DateTimeSelected -> select { copy(dateTime = intent.value) }
            is DateTypeIntent.DateFocusSelected -> select { copy(dateFocus = intent.value) }
            DateTypeIntent.TooltipToggled -> setState { copy(isTooltipPresented = !isTooltipPresented) }
            DateTypeIntent.TooltipDismissed -> setState { copy(isTooltipPresented = false) }
            DateTypeIntent.SaveClicked -> save()
            DateTypeIntent.SkipClicked -> skip()
            DateTypeIntent.ToastDismissed -> setState { copy(toast = null) }
        }
    }

    // 저장 중에는 선택을 받지 않는다. 응답을 기다리는 동안 값이 바뀌면 서버와 화면이 어긋난다
    private fun select(reducer: DateTypeState.() -> DateTypeState) {
        if (currentState.isSubmitting) return
        setState { reducer().copy(isTooltipPresented = false) }
    }

    private fun skip() {
        if (currentState.isSubmitting) return
        setState { copy(isTooltipPresented = false) }
        postSideEffect(DateTypeSideEffect.Finished)
    }

    private fun save() {
        val state = currentState
        val preference = state.datePreference ?: return
        if (!state.isSaveEnabled) return
        setState { copy(isSubmitting = true, isTooltipPresented = false, toast = null) }
        viewModelScope.launch {
            runCatching { profileRepository.updateDatePreference(preference) }
                .onSuccess {
                    setState { copy(isSubmitting = false) }
                    postSideEffect(DateTypeSideEffect.Finished)
                }
                .onFailure { error ->
                    setState { copy(isSubmitting = false) }
                    handleFailure(error)
                }
        }
    }

    private fun handleFailure(error: Throwable) {
        when (error) {
            ProfileError.Network -> setState { copy(toast = "네트워크 연결을 확인해 주세요.") }
            ProfileError.Unauthorized -> postSideEffect(DateTypeSideEffect.SessionExpired)
            else -> setState { copy(toast = "잠시 후 다시 시도해 주세요.") }
        }
    }
}
