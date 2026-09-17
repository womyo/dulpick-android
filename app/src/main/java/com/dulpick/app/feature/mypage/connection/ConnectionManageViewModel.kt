package com.dulpick.app.feature.mypage.connection

import androidx.lifecycle.viewModelScope
import com.dulpick.app.core.mvi.MviViewModel
import com.dulpick.app.domain.couple.CoupleError
import com.dulpick.app.domain.couple.CoupleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConnectionManageViewModel @Inject constructor(
    private val coupleRepository: CoupleRepository,
) : MviViewModel<ConnectionManageState, ConnectionManageIntent, ConnectionManageSideEffect>(
    ConnectionManageState(),
) {

    override fun onIntent(intent: ConnectionManageIntent) {
        when (intent) {
            ConnectionManageIntent.OnAppear -> load()
            ConnectionManageIntent.DisconnectClicked ->
                setState { copy(isDisconnectDialogPresented = true) }
            ConnectionManageIntent.DisconnectDismissed ->
                if (!currentState.isDisconnecting) setState { copy(isDisconnectDialogPresented = false) }
            ConnectionManageIntent.DisconnectConfirmed -> disconnect()
        }
    }

    private fun load() {
        viewModelScope.launch {
            runCatching { coupleRepository.current() }
                .onSuccess { status ->
                    status ?: return@onSuccess
                    setState {
                        copy(me = status.me, partner = status.partner, daysTogether = status.daysTogether)
                    }
                }
                .onFailure { error ->
                    if (error == CoupleError.Unauthorized) {
                        postSideEffect(ConnectionManageSideEffect.SessionExpired)
                    }
                }
        }
    }

    private fun disconnect() {
        if (currentState.isDisconnecting) return
        setState { copy(isDisconnecting = true) }
        viewModelScope.launch {
            runCatching { coupleRepository.disconnect() }
                .onSuccess {
                    setState { copy(isDisconnecting = false, isDisconnectDialogPresented = false) }
                    postSideEffect(ConnectionManageSideEffect.Disconnected)
                }
                .onFailure { error ->
                    setState { copy(isDisconnecting = false, isDisconnectDialogPresented = false) }
                    handleDisconnectFailure(error)
                }
        }
    }

    private fun handleDisconnectFailure(error: Throwable) {
        val message = when (error) {
            CoupleError.Unauthorized -> {
                postSideEffect(ConnectionManageSideEffect.SessionExpired)
                return
            }
            CoupleError.Network -> "네트워크 연결을 확인해 주세요."
            CoupleError.RateLimited -> "요청이 많아요. 잠시 후 다시 시도해 주세요."
            else -> "연결 해제에 실패했어요. 잠시 후 다시 시도해 주세요."
        }
        postSideEffect(ConnectionManageSideEffect.ShowToast(message))
    }
}
