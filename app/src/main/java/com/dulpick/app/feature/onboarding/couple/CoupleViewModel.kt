package com.dulpick.app.feature.onboarding.couple

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.dulpick.app.core.mvi.MviViewModel
import com.dulpick.app.domain.couple.Couple
import com.dulpick.app.domain.couple.CoupleError
import com.dulpick.app.domain.couple.CoupleRepository
import com.dulpick.app.domain.couple.CoupleStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

const val ARG_MY_NICKNAME = "myNickname"

// 온보딩은 건너뛰기 노출(기본 true), 마이페이지에서 연결하면 숨긴다(false)
const val ARG_COUPLE_SHOWS_SKIP = "showsSkip"

@HiltViewModel
class CoupleViewModel @Inject constructor(
    private val coupleRepository: CoupleRepository,
    savedStateHandle: SavedStateHandle,
) : MviViewModel<CoupleState, CoupleIntent, CoupleSideEffect>(
    CoupleState(
        myNickname = savedStateHandle.get<String>(ARG_MY_NICKNAME).orEmpty(),
        showsSkip = savedStateHandle.get<Boolean>(ARG_COUPLE_SHOWS_SKIP) ?: true,
    ),
) {

    override fun onIntent(intent: CoupleIntent) {
        when (intent) {
            CoupleIntent.OnAppear -> onAppear()
            CoupleIntent.SceneBecameActive -> checkConnectionIfNeeded()
            is CoupleIntent.CodeChanged -> changeCode(intent.code)
            CoupleIntent.ConnectClicked -> connect()
            CoupleIntent.CompleteClicked -> complete()
            CoupleIntent.BackClicked -> back()
            else -> handleSimpleIntent(intent)
        }
    }

    // 상태만 뒤집는 단순 인텐트. onIntent 복잡도를 낮추려 분리한다
    private fun handleSimpleIntent(intent: CoupleIntent) {
        when (intent) {
            CoupleIntent.RetryInviteCodeClicked -> if (!currentState.isLoadingInviteCode) loadInviteCode()
            CoupleIntent.SkipClicked -> setState { copy(isSkipConfirmPresented = true) }
            CoupleIntent.SkipConfirmed -> skipConfirmed()
            CoupleIntent.SkipConfirmDismissed -> setState { copy(isSkipConfirmPresented = false) }
            CoupleIntent.CodeInputClicked -> setState { copy(step = CoupleStep.CODE_INPUT, toast = null) }
            CoupleIntent.ToastDismissed -> setState { copy(toast = null) }
            else -> Unit
        }
    }

    private fun onAppear() {
        loadInviteCodeIfNeeded()
        checkConnectionIfNeeded()
    }

    private fun loadInviteCodeIfNeeded() {
        val state = currentState
        if (state.inviteCode == null && !state.isLoadingInviteCode) loadInviteCode()
    }

    private fun loadInviteCode() {
        setState { copy(isLoadingInviteCode = true, hasAttemptedInviteCode = true, inviteCodeError = null) }
        viewModelScope.launch {
            runCatching { coupleRepository.inviteCode() }
                .onSuccess { code ->
                    setState { copy(isLoadingInviteCode = false, inviteCode = code, inviteCodeError = null) }
                }
                .onFailure { error ->
                    setState { copy(isLoadingInviteCode = false) }
                    handleInviteCodeFailure(error)
                }
        }
    }

    private fun handleInviteCodeFailure(error: Throwable) {
        if (error == CoupleError.Unauthorized) {
            postSideEffect(CoupleSideEffect.SessionExpired)
            return
        }
        val message = if (error == CoupleError.Network) {
            "네트워크 연결을 확인해 주세요"
        } else {
            "잠시 후 다시 시도해 주세요"
        }
        setState { copy(inviteCodeError = message) }
    }

    private fun checkConnectionIfNeeded() {
        val state = currentState
        if (state.connectedCouple != null || state.isCheckingConnection) return
        setState { copy(isCheckingConnection = true) }
        viewModelScope.launch {
            runCatching { coupleRepository.current() }
                .onSuccess { status ->
                    setState { copy(isCheckingConnection = false) }
                    handleConnectionStatus(status)
                }
                .onFailure { error ->
                    setState { copy(isCheckingConnection = false) }
                    // 사용자가 누른 조회가 아니라 배경 확인이다. 인증 실패만 승격하고 나머진 삼킨다
                    if (error == CoupleError.Unauthorized) postSideEffect(CoupleSideEffect.SessionExpired)
                }
        }
    }

    private fun handleConnectionStatus(status: CoupleStatus?) {
        // 연결 요청과 조회가 겹쳐 응답하면 완료 화면이 두 장 쌓인다
        if (currentState.connectedCouple != null) return
        // 상대 정보가 비면 완료 화면 이름 칸이 빈다. 그 상태로는 안 넘긴다
        val partner = status?.takeIf { it.connected }?.partner ?: return
        setState { copy(connectedCouple = Couple(partner.nickname, partner.iconId), step = CoupleStep.COMPLETE) }
    }

    private fun skipConfirmed() {
        setState { copy(isSkipConfirmPresented = false) }
        postSideEffect(CoupleSideEffect.Finished)
    }

    // 입력칸이 잠겼다 풀리면 같은 값을 돌려보낸다. 그걸 입력으로 치면 방금 띄운 실패가 지워진다
    private fun changeCode(raw: String) {
        val normalized = CoupleState.normalizedCode(raw)
        if (normalized == currentState.code) return
        setState { copy(toast = null, code = normalized) }
    }

    private fun connect() {
        val state = currentState
        if (!state.isConnectEnabled) return
        val code = state.code
        setState { copy(isConnecting = true, toast = null) }
        viewModelScope.launch {
            runCatching { coupleRepository.connect(code) }
                .onSuccess { couple ->
                    setState { copy(isConnecting = false) }
                    if (currentState.connectedCouple != null) return@onSuccess
                    setState { copy(connectedCouple = couple, step = CoupleStep.COMPLETE) }
                }
                .onFailure { error ->
                    setState { copy(isConnecting = false) }
                    handleConnectFailure(error)
                }
        }
    }

    private fun handleConnectFailure(error: Throwable) {
        when (error) {
            CoupleError.InvalidInviteCode -> setState { copy(toast = "유효하지 않은 코드에요. 다시 확인해주세요") }
            CoupleError.AlreadyConnected -> setState { copy(toast = "이미 커플로 연결되어 있어요.") }
            CoupleError.RateLimited -> setState { copy(toast = "요청이 많아요. 잠시 후 다시 시도해 주세요.") }
            CoupleError.Network -> setState { copy(toast = "네트워크 연결을 확인해 주세요.") }
            CoupleError.Unauthorized -> postSideEffect(CoupleSideEffect.SessionExpired)
            else -> setState { copy(toast = "잠시 후 다시 시도해 주세요.") }
        }
    }

    private fun complete() {
        if (currentState.connectedCouple == null) return
        postSideEffect(CoupleSideEffect.Finished)
    }

    private fun back() {
        // 이전 화면에서 띄운 토스트는 경로가 바뀌면 유효하지 않다
        setState { copy(toast = null) }
        when (currentState.step) {
            // 코드 입력에서 뒤로 → 커플 첫 화면으로
            CoupleStep.CODE_INPUT -> setState { copy(step = CoupleStep.CONNECT) }
            // 첫 화면에서 뒤로 → 커플 구간을 떠나 닉네임으로
            CoupleStep.CONNECT -> postSideEffect(CoupleSideEffect.Back)
            // 완료 화면은 뒤로가기가 없다
            CoupleStep.COMPLETE -> Unit
        }
    }
}
