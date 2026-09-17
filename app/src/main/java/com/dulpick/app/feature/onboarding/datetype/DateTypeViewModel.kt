package com.dulpick.app.feature.onboarding.datetype

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.dulpick.app.core.mvi.MviViewModel
import com.dulpick.app.domain.profile.ProfileError
import com.dulpick.app.domain.profile.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

// 마이페이지에서 진입할 때 편집 모드로 여는 nav 인자
const val ARG_DATETYPE_EDIT = "edit"

@HiltViewModel
class DateTypeViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    savedStateHandle: SavedStateHandle,
) : MviViewModel<DateTypeState, DateTypeIntent, DateTypeSideEffect>(DateTypeState()) {

    // 편집 모드에서 현재 성향을 불러오는 동안 사용자가 먼저 선택했는지. iOS 는 부모가 성향을
    // 생성자로 주입해 비동기 로드가 없지만, 여기선 profile() 을 비동기로 읽어 늦은 응답이
    // 사용자 선택을 덮을 수 있다. 사용자가 이미 만졌으면 로드 결과를 버린다
    private var userEdited = false

    init {
        // 편집 모드(마이페이지 진입): 건너뛰기 숨기고 현재 성향을 미리 선택한다
        if (savedStateHandle.get<Boolean>(ARG_DATETYPE_EDIT) == true) {
            setState { copy(showsSkip = false) }
            loadCurrentPreference()
        }
    }

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
        }
    }

    // 저장 중에는 선택을 받지 않는다. 응답을 기다리는 동안 값이 바뀌면 서버와 화면이 어긋난다
    private fun select(reducer: DateTypeState.() -> DateTypeState) {
        if (currentState.isSubmitting) return
        userEdited = true
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
        setState { copy(isSubmitting = true, isTooltipPresented = false) }
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
            ProfileError.Network -> postSideEffect(DateTypeSideEffect.ShowToast("네트워크 연결을 확인해 주세요."))
            ProfileError.Unauthorized -> postSideEffect(DateTypeSideEffect.SessionExpired)
            else -> postSideEffect(DateTypeSideEffect.ShowToast("잠시 후 다시 시도해 주세요."))
        }
    }

    // 편집 모드에서 현재 저장된 성향을 불러와 4축을 미리 선택한다
    private fun loadCurrentPreference() {
        viewModelScope.launch {
            runCatching { profileRepository.profile() }
                .onSuccess { profile ->
                    // 로드가 끝나기 전에 사용자가 선택했다면 그 선택을 지키고 로드 결과는 버린다
                    if (userEdited) return@onSuccess
                    val preference = profile.datePreference ?: return@onSuccess
                    setState {
                        copy(
                            indoorOutdoor = preference.indoorOutdoor,
                            activityLevel = preference.activityLevel,
                            dateTime = preference.dateTime,
                            dateFocus = preference.dateFocus,
                        )
                    }
                }
                .onFailure { error ->
                    if (error == ProfileError.Unauthorized) {
                        postSideEffect(DateTypeSideEffect.SessionExpired)
                    }
                }
        }
    }
}
