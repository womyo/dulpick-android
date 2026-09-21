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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dulpick.app.feature.explore.ExploreScreen
import com.dulpick.app.feature.home.HomeScreen
import com.dulpick.app.core.map.KakaoMapView
import com.dulpick.app.domain.place.Place
import com.dulpick.app.feature.map.MapScreen
import com.dulpick.app.feature.mypage.MyPageScreen
import com.kakao.vectormap.KakaoMap
import com.dulpick.app.ui.theme.Colors
import com.dulpick.app.ui.theme.Typography

// 로그인·온보딩을 마치고 진입하는 메인 탭 컨테이너 (iOS MainTabView 대응).
// 상세 화면(예: 나의 데이트 유형)은 탭 밖(루트)에서 전체화면으로 push 한다 → onOpenDateType 로 위로 위임
@Composable
fun MainTabScreen(
    onLoggedOut: () -> Unit,
    actions: MainTabActions,
    // 지도 검색에서 되돌아온 검색 결과(JSON). 지도 탭이 읽어 검색 결과 모드에 들어간다
    pendingMapSearchArg: String? = null,
    onMapSearchConsumed: () -> Unit = {},
    // 검색 결과 모드에서 검색바 뒤로 → 그 검색어로 지도 검색을 다시 연다
    onReopenMapSearch: (String) -> Unit = {},
) {
    val tabNavController = rememberNavController()
    val backStackEntry by tabNavController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    // 홈에서 저장 장소 클릭 시 지도 탭이 읽어 상세를 여는 대상. 지도가 소비하면 비운다
    var pendingMapPlace by remember { mutableStateOf<Place?>(null) }
    // 탭 전환. 같은 탭 재선택은 무시하고 각 탭 스택 상태를 보존한다
    val navigateToTab: (String) -> Unit = { route ->
        tabNavController.navigate(route) {
            popUpTo(tabNavController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    // 지도 탭 여부. 지도는 이 값으로 재생/멈춤을 가른다
    val isMapTab = currentDestination?.hierarchy?.any { it.route == MainTab.MAP.route } == true
    // 상시 살아 있는 카카오 지도. 파괴 후 재시작이 안 되는 SDK 라 iOS 탭처럼 여기서 계속 들고 있는다
    var kakaoMap by remember { mutableStateOf<KakaoMap?>(null) }
    // 렌더링 재개 횟수. pause 중에 그린 내용은 프레임을 못 잡아, 재개될 때마다 지도 화면이 다시 그린다
    var mapRevision by remember { mutableStateOf(0) }

    Scaffold(
        containerColor = Colors.bgDefault,
        // 상단(상태바) 인셋은 각 화면이 직접 처리한다. 그래야 화면 배경이 상태바 뒤까지 그려진다
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = { MainBottomBar(currentDestination = currentDestination, onSelect = navigateToTab) },
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            // 지도는 NavHost 아래층에 상시 붙어 있다. 지도 탭이 아닐 땐 위 탭 화면(불투명)에 가려진다
            KakaoMapView(
                modifier = Modifier.fillMaxSize(),
                isActive = isMapTab,
                onMapReady = { kakaoMap = it },
                onResumed = { mapRevision++ },
            )
            MainTabNavHost(
                tabNavController = tabNavController,
                kakaoMap = kakaoMap,
                mapRevision = mapRevision,
                onLoggedOut = onLoggedOut,
                actions = actions,
                navigateToTab = navigateToTab,
                pendingMapSearchArg = pendingMapSearchArg,
                onMapSearchConsumed = onMapSearchConsumed,
                onReopenMapSearch = onReopenMapSearch,
                pendingMapPlace = pendingMapPlace,
                onMapPlaceChange = { pendingMapPlace = it },
            )
        }
    }
}

// 탭별 목적지. 지도 탭은 상시 지도 위에 오버레이(시트·검색바)만 그린다
@Suppress("LongParameterList")
@Composable
private fun MainTabNavHost(
    tabNavController: androidx.navigation.NavHostController,
    kakaoMap: KakaoMap?,
    mapRevision: Int,
    onLoggedOut: () -> Unit,
    actions: MainTabActions,
    navigateToTab: (String) -> Unit,
    pendingMapSearchArg: String?,
    onMapSearchConsumed: () -> Unit,
    onReopenMapSearch: (String) -> Unit,
    pendingMapPlace: Place?,
    onMapPlaceChange: (Place?) -> Unit,
) {
    NavHost(
        navController = tabNavController,
        startDestination = MainTab.HOME.route,
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
                        // 전체보기 → 지도 탭 이동만
                        onOpenMap = { navigateToTab(MainTab.MAP.route) },
                        // 장소 클릭 → 지도 탭 이동 + 그 장소 상세
                        onOpenPlaceOnMap = { place ->
                            onMapPlaceChange(place)
                            navigateToTab(MainTab.MAP.route)
                        },
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
                    MainTab.MAP -> MapScreen(
                        kakaoMap = kakaoMap,
                        mapRevision = mapRevision,
                        onSessionExpired = onLoggedOut,
                        onOpenSearch = actions.onOpenMapSearch,
                        pendingSearchArg = pendingMapSearchArg,
                        onSearchConsumed = onMapSearchConsumed,
                        onReopenSearch = onReopenMapSearch,
                        pendingPlace = pendingMapPlace,
                        onPlaceConsumed = { onMapPlaceChange(null) },
                    )
                    else -> TabPlaceholder(label = tab.label)
                }
            }
        }
    }
}

// 네이티브 Material3 탭바. containerColor 가 테마 surface(흰색)와 같으면 tonalElevation
// 톤 오버레이가 얹혀 틴트가 남으므로 elevation 을 0 으로 꺼 순백을 만든다
@Composable
private fun MainBottomBar(currentDestination: NavDestination?, onSelect: (String) -> Unit) {
    NavigationBar(containerColor = Colors.commonWhite, tonalElevation = 0.dp) {
        MainTab.entries.forEach { tab ->
            val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = { onSelect(tab.route) },
                icon = { Icon(painter = painterResource(tab.icon), contentDescription = tab.label) },
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
