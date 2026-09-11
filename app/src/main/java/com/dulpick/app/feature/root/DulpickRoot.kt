package com.dulpick.app.feature.root

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dulpick.app.feature.appintro.AppIntroScreen
import com.dulpick.app.feature.auth.AuthScreen
import com.dulpick.app.ui.component.AppButton
import com.dulpick.app.ui.component.AppButtonSize
import com.dulpick.app.ui.component.AppButtonVariant
import com.dulpick.app.ui.theme.Colors
import com.dulpick.app.ui.theme.Typography

@Composable
fun DulpickRoot(viewModel: RootViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    when (val current = state) {
        RootState.Loading -> SplashScreen()
        RootState.Error -> ErrorScreen(onRetry = viewModel::retry)
        is RootState.Ready -> DulpickNavHost(startRoute = current.start.route)
    }
}

@Composable
private fun DulpickNavHost(startRoute: String) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = startRoute) {
        composable(RootRoute.APP_INTRO.route) {
            AppIntroScreen(
                onFinished = {
                    navController.navigate(RootRoute.AUTH.route) {
                        popUpTo(RootRoute.APP_INTRO.route) { inclusive = true }
                    }
                },
            )
        }
        composable(RootRoute.AUTH.route) {
            AuthScreen(
                onLoginSucceeded = { _, isOnboardingCompleted ->
                    val target = if (isOnboardingCompleted) RootRoute.MAIN else RootRoute.ONBOARDING
                    navController.navigate(target.route) {
                        popUpTo(RootRoute.AUTH.route) { inclusive = true }
                    }
                },
            )
        }
        composable(RootRoute.ONBOARDING.route) { PlaceholderScreen(text = "온보딩 (예정)") }
        composable(RootRoute.MAIN.route) { PlaceholderScreen(text = "메인 (예정)") }
    }
}

@Composable
private fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Colors.commonWhite),
    )
}

// 세션 읽기 등 일시적 오류. 로그아웃으로 넘기지 않고 재시도를 제공한다
@Composable
private fun ErrorScreen(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Colors.commonWhite)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "일시적인 오류가 발생했어요.\n다시 시도해 주세요.",
            style = Typography.body1M,
            color = Colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        AppButton(
            text = "다시 시도",
            onClick = onRetry,
            variant = AppButtonVariant.DARK,
            size = AppButtonSize.LG,
            modifier = Modifier.padding(top = 20.dp),
        )
    }
}

@Composable
private fun PlaceholderScreen(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Colors.commonWhite),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = Typography.title3SB, color = Colors.textPrimary)
    }
}
