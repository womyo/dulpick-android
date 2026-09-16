package com.dulpick.app.feature.onboarding.nickname

import androidx.lifecycle.viewModelScope
import com.dulpick.app.core.mvi.MviViewModel
import com.dulpick.app.core.terms.TermsType
import com.dulpick.app.domain.profile.NotificationSettings
import com.dulpick.app.domain.profile.ProfileError
import com.dulpick.app.domain.profile.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NicknameViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
) : MviViewModel<NicknameState, NicknameIntent, NicknameSideEffect>(NicknameState()) {

    override fun onIntent(intent: NicknameIntent) {
        when (intent) {
            is NicknameIntent.NicknameChanged -> changeNickname(intent.value)
            NicknameIntent.NextClicked -> submit()
            NicknameIntent.BackClicked -> back()
            is NicknameIntent.TermsCheckTapped -> toggleTerms(intent.terms)
            NicknameIntent.TermsAgreeButtonTapped -> agreeTerms()
            is NicknameIntent.TermsDetailTapped -> openTermsDetail(intent.terms)
            NicknameIntent.ToastDismissed -> setState { copy(toast = null) }
        }
    }

    private fun changeNickname(value: String) {
        setState { copy(nickname = NicknameState.sanitize(value), inlineError = null) }
    }

    private fun toggleTerms(terms: TermsType) {
        setState {
            val next = if (terms in agreedTerms) agreedTerms - terms else agreedTerms + terms
            copy(agreedTerms = next)
        }
    }

    private fun agreeTerms() {
        setState {
            val agreed = if (isRequiredTermsAgreed) agreedTerms else NicknameState.SHEET_TERMS.toSet()
            copy(agreedTerms = agreed, isTermsSheetPresented = false)
        }
    }

    private fun openTermsDetail(terms: TermsType) {
        if (terms.url.startsWith("http")) {
            postSideEffect(NicknameSideEffect.OpenTermsUrl(terms.url))
        }
    }

    private fun submit() {
        val state = currentState
        if (!state.isNextEnabled) return
        val nickname = state.nickname
        val enablesMarketing = TermsType.MARKETING in state.agreedTerms
        setState { copy(isSubmitting = true, inlineError = null, toast = null) }
        viewModelScope.launch {
            runCatching { profileRepository.updateNickname(nickname, NicknameState.ICON_ID) }
                .onSuccess {
                    // 마케팅을 켠 채 제출했을 때만 알림 설정을 건드린다. 실패해도 화면은 다음으로 간다
                    if (enablesMarketing) enableMarketingNotification()
                    setState { copy(isSubmitting = false) }
                    postSideEffect(NicknameSideEffect.NicknameConfirmed(nickname))
                }
                .onFailure { error ->
                    setState { copy(isSubmitting = false) }
                    handleSubmitFailure(error)
                }
        }
    }

    private fun handleSubmitFailure(error: Throwable) {
        when (error) {
            ProfileError.InvalidNickname -> setState { copy(inlineError = "사용할 수 없는 닉네임이에요") }
            ProfileError.Network -> setState { copy(toast = "네트워크 연결을 확인해 주세요.") }
            ProfileError.Unauthorized -> postSideEffect(NicknameSideEffect.SessionExpired)
            else -> setState { copy(toast = "잠시 후 다시 시도해 주세요.") }
        }
    }

    // 조회로 현재 설정과 동의 버전을 받고 마케팅만 켜서 통째로 되돌려 보낸다.
    // 조회 실패·동의 버전 없음·변경 실패 무엇이든 화면은 다음으로 간다
    private suspend fun enableMarketingNotification() {
        val current = runCatching { profileRepository.notificationSettings() }.getOrNull() ?: return
        val consentVersion = current.availableMarketingConsentVersion ?: return
        val outgoing = NotificationSettings(
            contentSavedEnabled = current.contentSavedEnabled,
            dateScheduleEnabled = current.dateScheduleEnabled,
            marketingEnabled = true,
            marketingConsentVersion = consentVersion,
            availableMarketingConsentVersion = consentVersion,
        )
        runCatching { profileRepository.updateNotificationSettings(outgoing) }
    }

    private fun back() {
        if (currentState.isSubmitting) return
        postSideEffect(NicknameSideEffect.NavigateBack)
    }
}
