package com.dulpick.app.feature.mypage

import com.dulpick.app.core.mvi.UiIntent
import com.dulpick.app.core.mvi.UiSideEffect
import com.dulpick.app.core.mvi.UiState

data class MyPageState(
    // 프로필·알림을 아직 못 불러온 초기 상태
    val isLoading: Boolean = true,
    val nickname: String = "",
    val iconId: Int = DEFAULT_ICON_ID,
    val savedContentAlarm: Boolean = false,
    val dateScheduleAlarm: Boolean = false,
    val marketingAlarm: Boolean = false,
    // 알림 설정 전체 교체(PUT)에 실어 보낼 마케팅 동의 버전들
    val marketingConsentVersion: String? = null,
    val availableMarketingConsentVersion: String? = null,
    val isWithdrawDialogPresented: Boolean = false,
    val isWithdrawing: Boolean = false,
    val isProfileEditPresented: Boolean = false,
    val isSavingProfile: Boolean = false,
) : UiState {
    companion object {
        const val DEFAULT_ICON_ID = 1
    }
}

sealed interface MyPageIntent : UiIntent {
    data object OnAppear : MyPageIntent
    data class ContentSavedToggled(val enabled: Boolean) : MyPageIntent
    data class DateScheduleToggled(val enabled: Boolean) : MyPageIntent
    data class MarketingToggled(val enabled: Boolean) : MyPageIntent
    data object LogoutClicked : MyPageIntent
    data object WithdrawClicked : MyPageIntent
    data object WithdrawConfirmed : MyPageIntent
    data object WithdrawDismissed : MyPageIntent
    data object ProfileEditClicked : MyPageIntent
    data object ProfileEditDismissed : MyPageIntent
    data class ProfileSaveClicked(val nickname: String, val iconId: Int) : MyPageIntent

    // 연결 여부를 확인해 연결 관리 화면 / 커플 연결 플로우로 가른다
    data object ConnectionClicked : MyPageIntent
}

sealed interface MyPageSideEffect : UiSideEffect {
    // 로그아웃·세션 만료 모두 로그인으로 되돌린다
    data object LoggedOut : MyPageSideEffect
    data object SessionExpired : MyPageSideEffect
    data class ShowToast(val message: String) : MyPageSideEffect

    // 연결 관리 화면으로 (이미 연결됨)
    data object OpenConnection : MyPageSideEffect
    // 커플 연결 플로우로 (미연결). 완료 화면 닉네임 칸에 쓸 내 닉네임을 함께 넘긴다
    data class OpenCoupleConnect(val myNickname: String) : MyPageSideEffect
}
