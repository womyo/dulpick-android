package com.dulpick.app.feature.root

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
) : ViewModel() {

    private val _state = MutableStateFlow<RootState>(RootState.Loading)
    val state: StateFlow<RootState> = _state.asStateFlow()

    init {
        resolveStart()
    }

    @Suppress("TooGenericExceptionCaught")
    private fun resolveStart() {
        viewModelScope.launch {
            val route = try {
                val bootstrap = authRepository.restoreSession()
                when {
                    bootstrap == null -> RootRoute.AUTH
                    !bootstrap.isOnboardingCompleted -> RootRoute.ONBOARDING
                    else -> RootRoute.MAIN
                }
            } catch (error: Throwable) {
                // 세션 만료 등은 로그인부터 다시
                RootRoute.AUTH
            }
            _state.value = RootState.Ready(route)
        }
    }
}

sealed interface RootState {
    data object Loading : RootState
    data class Ready(val start: RootRoute) : RootState
}

enum class RootRoute(val route: String) {
    AUTH("auth"),
    ONBOARDING("onboarding"),
    MAIN("main"),
}
