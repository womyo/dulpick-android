package com.dulpick.app.feature.root

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dulpick.app.core.storage.AppIntroStore
import com.dulpick.app.domain.auth.AuthError
import com.dulpick.app.domain.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// 저장된 세션으로 시작 목적지를 정한다
@HiltViewModel
class RootViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val appIntroStore: AppIntroStore,
) : ViewModel() {

    private val _state = MutableStateFlow<RootState>(RootState.Loading)
    val state: StateFlow<RootState> = _state.asStateFlow()

    init {
        resolveStart()
    }

    // Error 상태에서 사용자가 다시 시도할 때 호출한다
    fun retry() {
        resolveStart()
    }

    @Suppress("TooGenericExceptionCaught")
    private fun resolveStart() {
        viewModelScope.launch {
            _state.value = RootState.Loading
            _state.value = try {
                val bootstrap = authRepository.restoreSession()
                val route = when {
                    bootstrap == null -> signedOutRoute()
                    !bootstrap.isOnboardingCompleted -> RootRoute.ONBOARDING
                    else -> RootRoute.MAIN
                }
                RootState.Ready(route)
            } catch (error: AuthError.Unauthorized) {
                // 세션 만료·무효만 로그인부터 다시
                RootState.Ready(signedOutRoute())
            } catch (error: Throwable) {
                // 저장소 읽기 실패 등 일시적 오류는 로그아웃으로 오인하지 말고 재시도 가능 상태로 둔다
                RootState.Error
            }
        }
    }

    // 비로그인일 때 첫 실행이면 인트로부터, 아니면 로그인부터. 인트로는 본 순간 봤다고 기록한다
    private suspend fun signedOutRoute(): RootRoute =
        if (appIntroStore.hasSeenAppIntro()) {
            RootRoute.AUTH
        } else {
            appIntroStore.markAppIntroSeen()
            RootRoute.APP_INTRO
        }
}

sealed interface RootState {
    data object Loading : RootState
    data class Ready(val start: RootRoute) : RootState

    // 세션 읽기 등 일시적 실패. 화면에서 재시도할 수 있다
    data object Error : RootState
}

enum class RootRoute(val route: String) {
    APP_INTRO("appIntro"),
    AUTH("auth"),
    ONBOARDING("onboarding"),
    DATETYPE("datetype"),
    MAIN("main"),
}
