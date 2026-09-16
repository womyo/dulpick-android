package com.dulpick.app.feature.onboarding.couple

import com.dulpick.app.core.mvi.UiIntent
import com.dulpick.app.core.mvi.UiSideEffect
import com.dulpick.app.core.mvi.UiState
import com.dulpick.app.domain.couple.Couple
import com.dulpick.app.domain.couple.InviteCode

// 커플 구간의 세 화면. 하나의 상태/ViewModel 을 공유하며 step 으로 전환한다
enum class CoupleStep { CONNECT, CODE_INPUT, COMPLETE }

data class CoupleState(
    val myNickname: String = "",
    val step: CoupleStep = CoupleStep.CONNECT,
    // 온보딩은 건너뛰기를 보여주고, 홈에서 진입하면 숨긴다
    val showsSkip: Boolean = true,
    val inviteCode: InviteCode? = null,
    val isLoadingInviteCode: Boolean = false,
    val hasAttemptedInviteCode: Boolean = false,
    val inviteCodeError: String? = null,
    val isSkipConfirmPresented: Boolean = false,
    val code: String = "",
    val isConnecting: Boolean = false,
    val connectedCouple: Couple? = null,
    val isCheckingConnection: Boolean = false,
    val toast: String? = null,
) : UiState {

    val isConnectEnabled: Boolean
        get() = code.length == CODE_LENGTH && !isConnecting && toast == null

    val partnerNickname: String
        get() = connectedCouple?.partnerNickname.orEmpty()

    companion object {
        // 초대 코드 자릿수. 다 차야 CTA 가 열리고 자동 제출은 없다
        const val CODE_LENGTH = 5

        // 영문·숫자만 남기고 대문자로 올린 뒤 자릿수만큼 자른다. 붙여넣기도 같은 경로를 탄다
        fun normalizedCode(raw: String): String =
            raw.filter { it.code in 0..127 && it.isLetterOrDigit() }
                .uppercase()
                .take(CODE_LENGTH)
    }
}

sealed interface CoupleIntent : UiIntent {
    data object OnAppear : CoupleIntent
    data object SceneBecameActive : CoupleIntent
    data object RetryInviteCodeClicked : CoupleIntent
    data object SkipClicked : CoupleIntent
    data object SkipConfirmed : CoupleIntent
    data object SkipConfirmDismissed : CoupleIntent
    data object CodeInputClicked : CoupleIntent
    data class CodeChanged(val code: String) : CoupleIntent
    data object ConnectClicked : CoupleIntent
    data object CompleteClicked : CoupleIntent
    data object BackClicked : CoupleIntent
    data object ToastDismissed : CoupleIntent
}

sealed interface CoupleSideEffect : UiSideEffect {
    // 커플 첫 화면에서 뒤로 → 온보딩(닉네임)으로 되돌린다. 구간 내 뒤로는 step 으로 처리한다
    data object Back : CoupleSideEffect
    // 구간을 벗어나 성향 선택으로 (연결 완료 or 건너뛰기)
    data object Finished : CoupleSideEffect
    data object SessionExpired : CoupleSideEffect
}
