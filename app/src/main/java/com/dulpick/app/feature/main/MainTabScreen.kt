package com.dulpick.app.feature.main

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dulpick.app.feature.explore.ExploreScreen
import com.dulpick.app.feature.home.HomeScreen
import com.dulpick.app.feature.mypage.MyPageScreen
import com.dulpick.app.ui.theme.Colors
import com.dulpick.app.ui.theme.Typography

// 로그인·온보딩을 마치고 진입하는 메인 탭 컨테이너 (iOS MainTabView 대응).
// 상세 화면(예: 나의 데이트 유형)은 탭 밖(루트)에서 전체화면으로 push 한다 → onOpenDateType 로 위로 위임
@Composable
fun MainTabScreen(
    onLoggedOut: () -> Unit,
    actions: MainTabActions,
) {
    val tabNavController = rememberNavController()
    val backStackEntry by tabNavController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        containerColor = Colors.bgDefault,
        // 상단(상태바) 인셋은 각 화면이 직접 처리한다. 그래야 화면 배경이 상태바 뒤까지 그려진다
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            // 네이티브 Material3 탭바. containerColor 가 테마 surface(흰색)와 같으면 tonalElevation
            // 톤 오버레이가 얹혀 틴트가 남으므로 elevation 을 0 으로 꺼 순백을 만든다
            NavigationBar(
                containerColor = Colors.commonWhite,
                tonalElevation = 0.dp,
            ) {
                MainTab.entries.forEach { tab ->
                    val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            // 같은 탭 재선택은 무시하고, 탭 전환 시 각 탭 스택 상태를 보존한다
                            tabNavController.navigate(tab.route) {
                                popUpTo(tabNavController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                painter = painterResource(tab.icon),
                                contentDescription = tab.label,
                            )
                        },
                        label = { Text(text = tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            // pill 없이 아이콘·글자만 핑크로. 인디케이터는 투명
                            selectedIconColor = Colors.primaryPink,
                            selectedTextColor = Colors.primaryPink,
                            indicatorColor = Color.Transparent,
                            unselectedIconColor = Colors.gray400,
                            unselectedTextColor = Colors.gray400,
                        ),
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = tabNavController,
            startDestination = MainTab.HOME.route,
            modifier = Modifier.padding(innerPadding),
            // 탭 전환은 기본 700ms 크로스페이드 대신 즉시 전환한다
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None },
        ) {
            MainTab.entries.forEach { tab ->
                composable(tab.route) {
                    when (tab) {
                        MainTab.HOME -> HomeScreen(
                            onSessionExpired = onLoggedOut,
                            onOpenCoupleConnect = actions.onOpenCoupleConnectFromHome,
                            onOpenPastDates = actions.onOpenPastDates,
                        )
                        MainTab.MY -> MyPageScreen(
                            onLoggedOut = onLoggedOut,
                            onOpenDateType = actions.onOpenDateType,
                            onOpenConnection = actions.onOpenConnection,
                            onOpenCoupleConnect = actions.onOpenCoupleConnect,
                        )
                        MainTab.EXPLORE -> ExploreScreen(
                            onSessionExpired = onLoggedOut,
                            onOpenSearch = actions.onOpenSearch,
                        )
                        else -> TabPlaceholder(label = tab.label)
                    }
                }
            }
        }
    }
}

// 탭별 화면은 아직 없다. 진입 확인용 임시 빈 화면
@Composable
private fun TabPlaceholder(label: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Colors.bgDefault),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "$label (예정)", style = Typography.title3SB, color = Colors.textPrimary)
    }
}
