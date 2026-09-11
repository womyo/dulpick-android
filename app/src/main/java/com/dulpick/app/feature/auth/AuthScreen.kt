package com.dulpick.app.feature.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dulpick.app.R
import com.dulpick.app.core.mvi.CollectSideEffect
import com.dulpick.app.domain.auth.AuthProvider
import com.dulpick.app.ui.theme.Colors
import com.dulpick.app.ui.theme.Typography
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    onLoginSucceeded: (userId: String, isOnboardingCompleted: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    CollectSideEffect(viewModel.sideEffect) { effect ->
        when (effect) {
            is AuthSideEffect.LoginSucceeded ->
                onLoginSucceeded(effect.userId, effect.isOnboardingCompleted)

            is AuthSideEffect.ShowError ->
                scope.launch { snackbarHostState.showSnackbar(effect.message) }
        }
    }

    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .fillMaxSize()
            .background(Colors.commonWhite),
    ) {
        AuthContent(state = state, onIntent = viewModel::onIntent)
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun AuthContent(
    state: AuthState,
    onIntent: (AuthIntent) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 80.dp, start = 20.dp, end = 20.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.brandwordmark),
                contentDescription = "Dulpick",
                modifier = Modifier.height(40.dp),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "우리 둘이서 만드는 데이트 둘픽",
                style = Typography.body1SB,
                color = Colors.textPrimary,
            )
        }

        Spacer(Modifier.weight(1f))

        Image(
            painter = painterResource(R.drawable.authillustration),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 26.dp)
                .sizeIn(maxWidth = 340.dp),
        )

        Spacer(Modifier.weight(1f))

        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Providers.forEach { provider ->
                SocialLoginButton(
                    provider = provider,
                    isLoading = state.loadingProvider == provider && state.isLoading,
                    isEnabled = !state.isLoading,
                    onClick = { onIntent(AuthIntent.LoginButtonClicked(provider)) },
                )
            }
        }

        Spacer(Modifier.height(20.dp))
    }
}

// Android 은 애플 로그인 제외
private val Providers = listOf(AuthProvider.KAKAO, AuthProvider.GOOGLE)
