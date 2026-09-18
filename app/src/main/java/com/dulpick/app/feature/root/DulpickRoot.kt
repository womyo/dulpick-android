package com.dulpick.app.feature.root

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dulpick.app.feature.appintro.AppIntroScreen
import com.dulpick.app.feature.auth.AuthScreen
import com.dulpick.app.feature.onboarding.couple.ARG_COUPLE_SHOWS_SKIP
import com.dulpick.app.feature.onboarding.couple.ARG_MY_NICKNAME
import com.dulpick.app.feature.onboarding.couple.CoupleScreen
import com.dulpick.app.feature.main.MainTabScreen
import com.dulpick.app.feature.mypage.connection.ConnectionManageScreen
import com.dulpick.app.feature.onboarding.datetype.ARG_DATETYPE_EDIT
import com.dulpick.app.feature.onboarding.datetype.DateTypeScreen
import com.dulpick.app.feature.onboarding.nickname.NicknameScreen
import com.dulpick.app.feature.search.SearchScreen
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

    NavHost(
        navController = navController,
        startDestination = startRoute,
        // 기본 700ms 크로스페이드가 어색해 즉시 전환으로 둔다. 슬라이드가 필요한 화면만 개별 지정
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None },
    ) {
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
        mainRoutes(navController = navController)
    }
}

// 로그인 후: 메인 탭 + 탭 밖 전체화면 상세(연결 관리·데이트 유형) 라우트
private fun NavGraphBuilder.mainRoutes(navController: NavHostController) {
    composable(RootRoute.MAIN.route) {
        MainTabScreen(
            // 로그아웃·세션 만료 → 백스택 전체를 비우고 로그인으로
            onLoggedOut = {
                navController.navigate(RootRoute.AUTH.route) {
                    popUpTo(navController.graph.id) { inclusive = true }
                }
            },
            // 상세는 탭 밖에서 전체화면 push
            onOpenDateType = { navController.navigate(MAIN_DATETYPE_ROUTE) },
            onOpenConnection = { navController.navigate(MAIN_CONNECTION_ROUTE) },
            onOpenCoupleConnect = { nickname ->
                navController.navigate("$MAIN_COUPLE_ROUTE_BASE/${Uri.encode(nickname)}")
            },
            onOpenSearch = { navController.navigate(MAIN_SEARCH_ROUTE) },
        )
    }
    composable(
        route = MAIN_SEARCH_ROUTE,
        enterTransition = { slideInHorizontally { it } },
        exitTransition = { slideOutHorizontally { -it / PARALLAX_DIVISOR } },
        popEnterTransition = { slideInHorizontally { -it / PARALLAX_DIVISOR } },
        popExitTransition = { slideOutHorizontally { it } },
    ) {
        SearchScreen(
            onBack = { navController.popBackStack() },
            onSessionExpired = { navController.navigateToAuth() },
        )
    }
    // 미연결 상태에서 마이페이지 "연결 관리" → 커플 연결 플로우(건너뛰기 없음)
    composable(
        route = "$MAIN_COUPLE_ROUTE_BASE/{$ARG_MY_NICKNAME}",
        arguments = listOf(
            navArgument(ARG_MY_NICKNAME) { type = NavType.StringType },
            navArgument(ARG_COUPLE_SHOWS_SKIP) {
                type = NavType.BoolType
                defaultValue = false
            },
        ),
        enterTransition = { slideInHorizontally { it } },
        exitTransition = { slideOutHorizontally { -it / PARALLAX_DIVISOR } },
        popEnterTransition = { slideInHorizontally { -it / PARALLAX_DIVISOR } },
        popExitTransition = { slideOutHorizontally { it } },
    ) {
        CoupleScreen(
            onBack = { navController.popBackStack() },
            // 연결 성공 → 커플 플로우를 걷어내고 연결 관리 화면으로 대체
            onFinished = {
                navController.navigate(MAIN_CONNECTION_ROUTE) {
                    popUpTo("$MAIN_COUPLE_ROUTE_BASE/{$ARG_MY_NICKNAME}") { inclusive = true }
                }
            },
            onSessionExpired = { navController.navigateToAuth() },
        )
    }
    composable(
        route = MAIN_CONNECTION_ROUTE,
        enterTransition = { slideInHorizontally { it } },
        exitTransition = { slideOutHorizontally { -it / PARALLAX_DIVISOR } },
        popEnterTransition = { slideInHorizontally { -it / PARALLAX_DIVISOR } },
        popExitTransition = { slideOutHorizontally { it } },
    ) {
        ConnectionManageScreen(
            onBack = { navController.popBackStack() },
            onSessionExpired = { navController.navigateToAuth() },
        )
    }
    composable(
        route = MAIN_DATETYPE_ROUTE,
        arguments = listOf(
            navArgument(ARG_DATETYPE_EDIT) {
                type = NavType.BoolType
                defaultValue = true
            },
        ),
        // 속도(animationSpec)는 slide* 함수의 기본 스프링에 맡긴다. 방향만 지정.
        // 오른쪽에서 슬라이드 인, 뒤로가기는 오른쪽으로 슬라이드 아웃 (뒤 화면은 살짝 패럴랙스)
        enterTransition = { slideInHorizontally { it } },
        exitTransition = { slideOutHorizontally { -it / PARALLAX_DIVISOR } },
        popEnterTransition = { slideInHorizontally { -it / PARALLAX_DIVISOR } },
        popExitTransition = { slideOutHorizontally { it } },
    ) {
        DateTypeScreen(
            onFinished = { navController.popBackStack() },
            onSessionExpired = { navController.navigateToAuth() },
            onBack = { navController.popBackStack() },
        )
    }
}

// 마이페이지에서 여는 전체화면 라우트 (탭 밖 push)
private const val MAIN_DATETYPE_ROUTE = "main/datetype"
private const val MAIN_CONNECTION_ROUTE = "main/connection"
private const val MAIN_COUPLE_ROUTE_BASE = "main/couple"
private const val MAIN_SEARCH_ROUTE = "main/search"
// 뒤 화면이 살짝 따라 밀리는 패럴랙스 정도(1/4)
private const val PARALLAX_DIVISOR = 4

// 커플 연결. myNickname 을 경로 인자로 넘겨 완료 화면 닉네임 칸에 쓴다
private const val ROUTE_COUPLE_BASE = "couple"

// 세션 만료·온보딩 이탈 공통. 보호 화면(검색·상세 등)이나 온보딩 화면(예: DATETYPE 세션 만료)에서
// 호출되든 백스택 전체를 비우고 로그인만 남긴다. ONBOARDING 은 navigateToDateType 시점에 이미
// 제거될 수 있어 그 지점까지만 지우면 pop 이 안 되므로 그래프 전체를 대상으로 한다
private fun androidx.navigation.NavController.navigateToAuth() {
    navigate(RootRoute.AUTH.route) {
        popUpTo(this@navigateToAuth.graph.id) { inclusive = true }
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
