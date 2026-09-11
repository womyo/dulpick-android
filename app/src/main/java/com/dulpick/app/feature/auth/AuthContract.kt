package com.dulpick.app.feature.auth

import com.dulpick.app.core.mvi.UiIntent
import com.dulpick.app.core.mvi.UiSideEffect
import com.dulpick.app.core.mvi.UiState
import com.dulpick.app.domain.auth.AuthProvider

data class AuthState(
    val isLoading: Boolean = false,
    val loadingProvider: AuthProvider? = null,
) : UiState

sealed interface AuthIntent : UiIntent {
    data class LoginButtonClicked(val provider: AuthProvider) : AuthIntent
}

sealed interface AuthSideEffect : UiSideEffect {
    data class LoginSucceeded(
        val userId: String,
        val isOnboardingCompleted: Boolean,
    ) : AuthSideEffect

    // 로컬 에러 토스트
    data class ShowError(val message: String) : AuthSideEffect
}
