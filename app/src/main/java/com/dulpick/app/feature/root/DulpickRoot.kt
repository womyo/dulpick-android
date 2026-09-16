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
import android.net.Uri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dulpick.app.feature.appintro.AppIntroScreen
import com.dulpick.app.feature.auth.AuthScreen
import com.dulpick.app.feature.onboarding.couple.ARG_MY_NICKNAME
import com.dulpick.app.feature.onboarding.couple.CoupleScreen
import com.dulpick.app.feature.main.MainTabScreen
import com.dulpick.app.feature.onboarding.datetype.DateTypeScreen
import com.dulpick.app.feature.onboarding.nickname.NicknameScreen
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
        composable(RootRoute.ONBOARDING.route) {
            NicknameScreen(
                onConfirmed = { nickname ->
                    navController.navigate("$ROUTE_COUPLE_BASE/${Uri.encode(nickname)}")
                },
                // TODO: iOS 는 첫 온보딩 화면에서 뒤로 가면 로그아웃한다. 지금은 로그인으로만 되돌린다
                onBack = { navController.navigateToAuth() },
            )
        }
        composable(
            route = "$ROUTE_COUPLE_BASE/{$ARG_MY_NICKNAME}",
            arguments = listOf(navArgument(ARG_MY_NICKNAME) { type = NavType.StringType }),
        ) {
            CoupleScreen(
                // 커플 첫 화면에서 뒤로 → 닉네임으로
                onBack = { navController.popBackStack() },
                onFinished = { navController.navigateToDateType() },
                onSessionExpired = { navController.navigateToAuth() },
            )
        }
        composable(RootRoute.DATETYPE.route) {
            DateTypeScreen(
                onFinished = {
                    navController.navigate(RootRoute.MAIN.route) {
                        popUpTo(RootRoute.DATETYPE.route) { inclusive = true }
                    }
                },
                onSessionExpired = { navController.navigateToAuth() },
            )
        }
        composable(RootRoute.MAIN.route) { MainTabScreen() }
    }
}

// 커플 연결. myNickname 을 경로 인자로 넘겨 완료 화면 닉네임 칸에 쓴다
private const val ROUTE_COUPLE_BASE = "couple"

private fun androidx.navigation.NavController.navigateToAuth() {
    navigate(RootRoute.AUTH.route) {
        popUpTo(RootRoute.ONBOARDING.route) { inclusive = true }
    }
}

// 성향 선택 진입 시 커플·닉네임을 스택에서 걷어낸다. 성향에서 뒤로 돌아갈 곳을 두지 않는다
private fun androidx.navigation.NavController.navigateToDateType() {
    navigate(RootRoute.DATETYPE.route) {
        popUpTo(RootRoute.ONBOARDING.route) { inclusive = true }
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
