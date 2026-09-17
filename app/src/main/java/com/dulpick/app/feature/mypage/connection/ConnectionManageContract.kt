package com.dulpick.app.feature.mypage.connection

import com.dulpick.app.core.mvi.UiIntent
import com.dulpick.app.core.mvi.UiSideEffect
import com.dulpick.app.core.mvi.UiState
import com.dulpick.app.domain.couple.CoupleMember

data class ConnectionManageState(
    val me: CoupleMember? = null,
    val partner: CoupleMember? = null,
    val daysTogether: Int? = null,
    val isDisconnectDialogPresented: Boolean = false,
    val isDisconnecting: Boolean = false,
) : UiState

sealed interface ConnectionManageIntent : UiIntent {
    data object OnAppear : ConnectionManageIntent
    data object DisconnectClicked : ConnectionManageIntent
    data object DisconnectConfirmed : ConnectionManageIntent
    data object DisconnectDismissed : ConnectionManageIntent
}

sealed interface ConnectionManageSideEffect : UiSideEffect {
    // 연결 해제 성공 → 뒤로
    data object Disconnected : ConnectionManageSideEffect
    data object SessionExpired : ConnectionManageSideEffect
    data class ShowToast(val message: String) : ConnectionManageSideEffect
}
