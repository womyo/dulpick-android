package com.dulpick.app.feature.main

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
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
import com.dulpick.app.feature.search.SearchScreen
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

    // 검색(지도)에서 장소를 골라 돌아오면 지도 탭으로 옮겨 소비하게 한다 (iOS selectedTab = .map 대응)
    LaunchedEffect(pendingMapSearchArg) {
        if (pendingMapSearchArg != null && !isMapTab) navigateToTab(MainTab.MAP.route)
    }
    // 상시 살아 있는 카카오 지도. 파괴 후 재시작이 안 되는 SDK 라 iOS 탭처럼 여기서 계속 들고 있는다
    var kakaoMap by remember { mutableStateOf<KakaoMap?>(null) }
    // 렌더링 재개 횟수. pause 중에 그린 내용은 프레임을 못 잡아, 재개될 때마다 지도 화면이 다시 그린다
    var mapRevision by remember { mutableStateOf(0) }
    // 탭바가 차지하는 높이 = Material 탭바 높이 + 시스템 내비게이션 인셋(탭바가 스스로 더한다)
    val tabBarHeight = NAVIGATION_BAR_HEIGHT +
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // 탭바는 Scaffold 가 아니라 각 탭 화면 안에 둔다. Scaffold 의 bottomBar 는 항상 맨 위층이라
    // 검색 화면이 탭바까지 덮으며 밀려 들어오는(다른 화면 push 와 같은) 전환을 만들 수 없다
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Colors.bgDefault),
    ) {
        // 지도는 NavHost 아래층에 상시 붙어 있다. 지도 탭이 아닐 땐 위 탭 화면(불투명)에 가려진다.
        // 탭바 높이를 미리 빼 둔다. 탭바가 측정된 뒤에 맞추면 지도 크기가 한 번 바뀌어,
        // 크기 변경에 예민한 지도 엔진이 빈 화면을 그릴 수 있다
        KakaoMapView(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = tabBarHeight),
            isActive = isMapTab,
            onMapReady = { kakaoMap = it },
            onResumed = { mapRevision++ },
        )
        // 지도 탭이 아니면 지도를 덮어 둔다. 화면 전환 중 위 화면이 잠깐 비는 순간에도
        // 아래층 지도가 비쳐 보이지 않게 한다
        if (!isMapTab) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Colors.bgDefault),
            )
        }
        MainTabNavHost(
            tabNavController = tabNavController,
            kakaoMap = kakaoMap,
            mapRevision = mapRevision,
            onLoggedOut = onLoggedOut,
            actions = actions,
            navigateToTab = navigateToTab,
            currentDestination = currentDestination,
            pendingMapSearchArg = pendingMapSearchArg,
            onMapSearchConsumed = onMapSearchConsumed,
            onReopenMapSearch = onReopenMapSearch,
            pendingMapPlace = pendingMapPlace,
            onMapPlaceChange = { pendingMapPlace = it },
        )
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
    currentDestination: NavDestination?,
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
            composable(
                route = tab.route,
                // 탭 전환은 즉시. 검색 화면과 주고받을 때만 다른 화면 push/pop 과 같은 전환을 쓴다
                enterTransition = { fromSearch { slideInHorizontally { it } } },
                exitTransition = { toSearch { slideOutHorizontally { -it / PARALLAX_DIVISOR } } },
                popEnterTransition = { fromSearch { slideInHorizontally { -it / PARALLAX_DIVISOR } } },
                popExitTransition = { toSearch { slideOutHorizontally { it } } },
            ) {
                // 탭바를 탭 화면 안에 둔다. 그래야 검색 화면이 밀려 들어올 때 탭바가 화면과 함께 밀린다
                TabWithBottomBar(currentDestination = currentDestination, onSelectTab = navigateToTab) {
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
                        // 검색은 탭 안에 push 한다. 지도 상세를 보러 가도 이 화면이 스택에 그대로 남는다
                        onOpenSearch = { tabNavController.navigate(EXPLORE_SEARCH_ROUTE) },
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
        // 탐색 검색. 탭바가 없는 전체 화면이라 다른 화면 push 와 똑같이 밀려 들어오고 밀려 나간다
        composable(
            route = EXPLORE_SEARCH_ROUTE,
            enterTransition = { slideInHorizontally { it } },
            exitTransition = { slideOutHorizontally { -it / PARALLAX_DIVISOR } },
            popEnterTransition = { slideInHorizontally { -it / PARALLAX_DIVISOR } },
            popExitTransition = { slideOutHorizontally { it } },
        ) {
            SearchScreen(
                onBack = { tabNavController.popBackStack() },
                onSessionExpired = onLoggedOut,
            )
        }
    }
}

// 탐색 탭 안의 검색 화면
private const val EXPLORE_SEARCH_ROUTE = "main/explore/search"
// 뒤 화면이 살짝 따라 밀리는 패럴랙스 정도(1/4). 루트 화면 전환과 같은 값
private const val PARALLAX_DIVISOR = 4
// Material3 NavigationBar 의 높이(NavigationBarTokens.ContainerHeight). 시스템 인셋은 별도로 더한다
private val NAVIGATION_BAR_HEIGHT = 80.dp

// 검색 화면에서 들어오는 전환이면 주어진 애니메이션을, 탭 전환이면 즉시 전환을 쓴다
private fun AnimatedContentTransitionScope<NavBackStackEntry>.fromSearch(
    transition: () -> EnterTransition,
): EnterTransition =
    if (initialState.destination.route == EXPLORE_SEARCH_ROUTE) transition() else EnterTransition.None

// 검색 화면으로 나가는 전환이면 주어진 애니메이션을, 탭 전환이면 즉시 전환을 쓴다
private fun AnimatedContentTransitionScope<NavBackStackEntry>.toSearch(
    transition: () -> ExitTransition,
): ExitTransition =
    if (targetState.destination.route == EXPLORE_SEARCH_ROUTE) transition() else ExitTransition.None

// 탭 화면 + 그 아래 탭바. 탭바가 화면에 붙어 있어 검색 화면이 밀려 들어올 때 함께 밀린다
@Composable
private fun TabWithBottomBar(
    currentDestination: NavDestination?,
    onSelectTab: (String) -> Unit,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) { content() }
        MainBottomBar(currentDestination = currentDestination, onSelect = onSelectTab)
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
