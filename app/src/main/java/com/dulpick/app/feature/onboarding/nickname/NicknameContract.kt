package com.dulpick.app.feature.onboarding.nickname

import com.dulpick.app.core.mvi.UiIntent
import com.dulpick.app.core.mvi.UiSideEffect
import com.dulpick.app.core.mvi.UiState
import com.dulpick.app.core.terms.TermsType

data class NicknameState(
    val nickname: String = "",
    val isSubmitting: Boolean = false,
    val inlineError: String? = null,
    val toast: String? = null,
    // 온보딩 진입 시 약관 시트를 먼저 띄운다
    val isTermsSheetPresented: Boolean = true,
    val agreedTerms: Set<TermsType> = emptySet(),
) : UiState {

    val isNextEnabled: Boolean
        get() = nickname.length in 1..MAX_NICKNAME_LENGTH && !isSubmitting

    val lengthError: String?
        get() = if (nickname.length > MAX_NICKNAME_LENGTH) "최대 6글자 내로 입력해주세요" else null

    // 필수 약관 둘이 모두 켜졌는지
    val isRequiredTermsAgreed: Boolean
        get() = SHEET_TERMS.filter { it.isRequired }.all { it in agreedTerms }

    // 필수가 다 켜지면 그대로 닫는 버튼, 아니면 셋을 한 번에 켜는 버튼
    val termsAgreeButtonTitle: String
        get() = if (isRequiredTermsAgreed) "완료" else "모두 동의하기"

    companion object {
        const val MAX_NICKNAME_LENGTH = 6

        // 시안에 프로필 아이콘 선택이 없어 서버 계약상 필요한 값만 고정으로 보낸다
        const val ICON_ID = 1

        // 초과 사실을 보여주려고 기준보다 한 글자만 더 받는다
        const val MAX_INPUT_LENGTH = 7

        // 시트가 그리는 동의 항목과 표시 순서
        val SHEET_TERMS = listOf(TermsType.SERVICE, TermsType.PRIVACY, TermsType.MARKETING)

        // 공백은 담지 않고 길이도 여기서 잘라 nickname 을 항상 검증 가능한 값으로 둔다
        fun sanitize(nickname: String): String =
            nickname.filter { !it.isWhitespace() }.take(MAX_INPUT_LENGTH)
    }
}

sealed interface NicknameIntent : UiIntent {
    data class NicknameChanged(val value: String) : NicknameIntent
    data object NextClicked : NicknameIntent
    data object BackClicked : NicknameIntent
    data class TermsCheckTapped(val terms: TermsType) : NicknameIntent
    data object TermsAgreeButtonTapped : NicknameIntent
    data class TermsDetailTapped(val terms: TermsType) : NicknameIntent
    data object ToastDismissed : NicknameIntent
}

sealed interface NicknameSideEffect : UiSideEffect {
    data class NicknameConfirmed(val nickname: String) : NicknameSideEffect
    data object NavigateBack : NicknameSideEffect
    // 세션 만료. 로그인으로 되돌린다
    data object SessionExpired : NicknameSideEffect
    data class OpenTermsUrl(val url: String) : NicknameSideEffect
}
