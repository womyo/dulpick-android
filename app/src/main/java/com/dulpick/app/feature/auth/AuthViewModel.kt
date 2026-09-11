package com.dulpick.app.feature.auth

import androidx.lifecycle.viewModelScope
import com.dulpick.app.core.mvi.MviViewModel
import com.dulpick.app.domain.auth.AuthError
import com.dulpick.app.domain.auth.AuthProvider
import com.dulpick.app.domain.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : MviViewModel<AuthState, AuthIntent, AuthSideEffect>(AuthState()) {

    override fun onIntent(intent: AuthIntent) {
        when (intent) {
            is AuthIntent.LoginButtonClicked -> login(intent.provider)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun login(provider: AuthProvider) {
        if (currentState.isLoading) return
        setState { copy(isLoading = true, loadingProvider = provider) }

        viewModelScope.launch {
            try {
                val bootstrap = authRepository.login(provider)
                setState { copy(isLoading = false, loadingProvider = null) }
                postSideEffect(
                    AuthSideEffect.LoginSucceeded(
                        userId = bootstrap.session.userId,
                        isOnboardingCompleted = bootstrap.isOnboardingCompleted,
                    ),
                )
            } catch (error: Throwable) {
                setState { copy(isLoading = false, loadingProvider = null) }
                val authError = error as? AuthError ?: AuthError.Unknown
                toastMessage(authError)?.let { postSideEffect(AuthSideEffect.ShowError(it)) }
            }
        }
    }

    // 취소는 조용히 넘긴다
    private fun toastMessage(error: AuthError): String? = when (error) {
        AuthError.Cancelled -> null
        AuthError.Network -> "네트워크 연결을 확인해 주세요."
        AuthError.LoginFailed -> "로그인에 실패했습니다."
        AuthError.Unauthorized, AuthError.Storage, AuthError.Unknown -> "잠시 후 다시 시도해 주세요."
    }
}
