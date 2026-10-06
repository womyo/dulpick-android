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
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.unit.Dp
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
import com.dulpick.app.domain.place.Place
import com.dulpick.app.feature.map.DetailTarget
import com.dulpick.app.feature.map.MapScreen
import com.dulpick.app.feature.map.rememberMapSheetState
import com.dulpick.app.feature.mypage.MyPageScreen
import com.dulpick.app.feature.search.SearchScreen
import com.dulpick.app.ui.theme.Colors
import com.dulpick.app.ui.theme.Typography

// 로그인·온보딩을 마치고 진입하는 메인 탭 컨테이너 (iOS MainTabView 대응).
// 상세 화면(예: 나의 데이트 유형)은 탭 밖(루트)에서 전체화면으로 push 한다 → onOpenDateType 로 위로 위임
@OptIn(ExperimentalMaterial3Api::class)
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
    // 탐색 검색에서 고른 장소. 지도가 상세 전용(content) 모드로 열고 비운다
    var pendingContentDetail by remember { mutableStateOf<DetailTarget?>(null) }
    // 탐색·검색·홈에서 고른 게시물. 지도가 그 게시글 상세를 열고 비운다
    var pendingPostId by remember { mutableStateOf<String?>(null) }
    // 그 게시글을 닫으면 돌아갈 곳. 탭이면 그 탭으로, 검색 화면이면 pop 한다
    var postReturnTab by remember { mutableStateOf<String?>(null) }
    // 지도에 검색 결과·장소 상세 시트가 떠 있는지. 떠 있으면 탭바를 감춘다
    var mapHidesTabBar by remember { mutableStateOf(false) }
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
    // 지도 시트 상태를 탭 밖에 둔다. 지도 화면 안에 두면 탭을 옮길 때마다 새로 만들어지고,
    // 새 상태는 첫 레이아웃 전까지 위치가 없어 시트가 화면 맨 위에서 제자리로 떨어진다
    val mapSheetState = rememberMapSheetState()
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
        MainTabNavHost(
            tabNavController = tabNavController,
            mapSheetState = mapSheetState,
            onLoggedOut = onLoggedOut,
            actions = actions,
            navigateToTab = navigateToTab,
            currentDestination = currentDestination,
            tabBarHeight = tabBarHeight,
            search = MapTabSearch(
                pendingArg = pendingMapSearchArg,
                onConsumed = onMapSearchConsumed,
                onReopen = onReopenMapSearch,
            ),
            pending = MapTabPending(
                contentDetail = pendingContentDetail,
                onContentDetailChange = { pendingContentDetail = it },
                place = pendingMapPlace,
                onPlaceChange = { pendingMapPlace = it },
                postId = pendingPostId,
                onPostIdChange = { pendingPostId = it },
                postReturnTab = postReturnTab,
                onPostReturnTabChange = { postReturnTab = it },
            ),
            mapHidesTabBar = mapHidesTabBar,
            onMapHidesTabBarChange = { mapHidesTabBar = it },
        )
    }
}

// 탭별 목적지. 지도 탭은 상시 지도 위에 오버레이(시트·검색바)만 그린다
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongParameterList")
@Composable
private fun MainTabNavHost(
    tabNavController: androidx.navigation.NavHostController,
    onLoggedOut: () -> Unit,
    actions: MainTabActions,
    navigateToTab: (String) -> Unit,
    currentDestination: NavDestination?,
    tabBarHeight: Dp,
    mapSheetState: BottomSheetScaffoldState,
    search: MapTabSearch,
    pending: MapTabPending,
    mapHidesTabBar: Boolean,
    onMapHidesTabBarChange: (Boolean) -> Unit,
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
                // 검색 결과·장소 상세 시트가 뜬 지도에는 탭바가 없다. 검색 위에 얹힌 지도(상세 전용)는
                // 상세가 올라오기 전에도 탭바가 보이지 않아야 해 백스택으로 미리 가린다
                val overSearch = tabNavController.previousBackStackEntry
                    ?.destination?.route == EXPLORE_SEARCH_ROUTE
                val hideBar = tab == MainTab.MAP && (overSearch || mapHidesTabBar)
                // 탭바를 탭 화면 안에 둔다. 그래야 검색 화면이 밀려 들어올 때 탭바가 화면과 함께 밀린다
                TabWithBottomBar(
                    showBar = !hideBar,
                    currentDestination = currentDestination,
                    onSelectTab = navigateToTab,
                ) {
                when (tab) {
                    MainTab.HOME -> HomeScreen(
                        onOpenContent = { id ->
                            pending.onPostIdChange(id)
                            pending.onPostReturnTabChange(MainTab.HOME.route)
                            navigateToTab(MainTab.MAP.route)
                        },
                        onSessionExpired = onLoggedOut,
                        onOpenCoupleConnect = actions.onOpenCoupleConnectFromHome,
                        onOpenPastDates = actions.onOpenPastDates,
                        // 전체보기 → 지도 탭 이동만
                        onOpenMap = { navigateToTab(MainTab.MAP.route) },
                        // 장소 클릭 → 지도 탭 이동 + 그 장소 상세
                        onOpenPlaceOnMap = { place ->
                            pending.onPlaceChange(place)
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
                        onOpenContent = { id ->
                            pending.onPostIdChange(id)
                            pending.onPostReturnTabChange(MainTab.EXPLORE.route)
                            navigateToTab(MainTab.MAP.route)
                        },
                        // 검색은 탭 안에 push 한다. 지도 상세를 보러 가도 이 화면이 스택에 그대로 남는다
                        onOpenSearch = { tabNavController.navigate(EXPLORE_SEARCH_ROUTE) },
                    )
                    MainTab.MAP -> MapTab(
                        tabBarHeight = tabBarHeight,
                        hideBar = hideBar,
                        mapSheetState = mapSheetState,
                        onLoggedOut = onLoggedOut,
                        navigateToTab = navigateToTab,
                        tabNavController = tabNavController,
                        actions = actions,
                        search = search,
                        pending = pending,
                        onTabBarHiddenChange = onMapHidesTabBarChange,
                    )
                    else -> TabPlaceholder(label = tab.label)
                }
                }
            }
        }
        exploreSearchDestination(
            tabNavController = tabNavController,
            onLoggedOut = onLoggedOut,
            onContentDetailChange = pending.onContentDetailChange,
            onPostIdChange = pending.onPostIdChange,
            onPostReturnTabChange = pending.onPostReturnTabChange,
        )
    }
}

// 지도 탭에 넘기는 검색 관련 값 묶음
private data class MapTabSearch(
    val pendingArg: String?,
    val onConsumed: () -> Unit,
    val onReopen: (String) -> Unit,
)

// 다른 탭·화면에서 지도로 넘겨 둔 대기 값 묶음
private data class MapTabPending(
    val contentDetail: DetailTarget?,
    val onContentDetailChange: (DetailTarget?) -> Unit,
    val place: Place?,
    val onPlaceChange: (Place?) -> Unit,
    val postId: String?,
    val onPostIdChange: (String?) -> Unit,
    val postReturnTab: String?,
    val onPostReturnTabChange: (String?) -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongParameterList")
private fun MapTab(
    tabBarHeight: Dp,
    hideBar: Boolean,
    mapSheetState: BottomSheetScaffoldState,
    onLoggedOut: () -> Unit,
    navigateToTab: (String) -> Unit,
    tabNavController: androidx.navigation.NavHostController,
    actions: MainTabActions,
    search: MapTabSearch,
    pending: MapTabPending,
    onTabBarHiddenChange: (Boolean) -> Unit,
) {
    MapScreen(
        tabBarHeight = tabBarHeight,
        sheetState = mapSheetState,
        onSessionExpired = onLoggedOut,
        onOpenSearch = actions.onOpenMapSearch,
        pendingSearchArg = search.pendingArg,
        onSearchConsumed = search.onConsumed,
        onReopenSearch = search.onReopen,
        pendingContentDetail = pending.contentDetail,
        onContentDetailConsumed = { pending.onContentDetailChange(null) },
        // 상세 전용(content) 모드는 검색 화면 위에 얹힌 것이라, 닫으면 그대로 pop 한다.
        // 검색 화면이 스택에 살아 있어 결과 리스트가 그 자리에 다시 보인다
        onCloseContentDetail = { tabNavController.popBackStack() },
        onTabBarHiddenChange = onTabBarHiddenChange,
        // 탭바를 감췄으니 그만큼 시트를 키워, 탭바 있을 때와 같은 높이까지 펼쳐지게 한다
        sheetBottomInset = if (hideBar) tabBarHeight else 0.dp,
        pendingPlace = pending.place,
        onPlaceConsumed = { pending.onPlaceChange(null) },
        pendingPostId = pending.postId,
        onPostConsumed = { pending.onPostIdChange(null) },
        // 탭에서 왔으면 그 탭으로, 검색 화면에서 왔으면 pop 한다
        onClosePostDetail = {
            val tab = pending.postReturnTab
            pending.onPostReturnTabChange(null)
            if (tab != null) navigateToTab(tab) else tabNavController.popBackStack()
        },
    )
}

// 탐색 검색. 장소 결과를 탭하면 이 화면을 남겨 둔 채 지도를 그 위에 올려 상세만 보여준다
// (iOS showPlaceDetail → presentSearchPlaceDetail 대응).
// 탭바가 없는 전체 화면이라 다른 화면 push 와 똑같이 밀려 들어오고 밀려 나간다
private fun androidx.navigation.NavGraphBuilder.exploreSearchDestination(
    tabNavController: androidx.navigation.NavHostController,
    onLoggedOut: () -> Unit,
    onContentDetailChange: (DetailTarget?) -> Unit,
    onPostIdChange: (String?) -> Unit,
    onPostReturnTabChange: (String?) -> Unit,
) {
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
            onOpenPlaceOnMap = { query, place ->
                onContentDetailChange(
                    DetailTarget(place, query = query, serverPlaceId = null, contentMode = true),
                )
                tabNavController.navigate(MainTab.MAP.route) { launchSingleTop = true }
            },
            // 게시물은 이 화면을 스택에 남긴 채 지도를 올려 게시글 상세만 보여준다
            onOpenContent = { id ->
                onPostIdChange(id)
                onPostReturnTabChange(null)
                tabNavController.navigate(MainTab.MAP.route) { launchSingleTop = true }
            },
        )
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

// 탭 화면 + 그 아래 탭바. 탭바가 화면에 붙어 있어 검색 화면이 밀려 들어올 때 함께 밀린다.
// showBar 가 false 면 탭바 없는 전체 화면(검색에서 올라온 장소 상세)
@Composable
private fun TabWithBottomBar(
    showBar: Boolean,
    currentDestination: NavDestination?,
    onSelectTab: (String) -> Unit,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) { content() }
        if (showBar) {
            MainBottomBar(currentDestination = currentDestination, onSelect = onSelectTab)
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
