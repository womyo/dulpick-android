package com.dulpick.app.feature.root

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import android.net.Uri
import androidx.hilt.navigation.compose.hiltViewModel
import com.dulpick.app.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dulpick.app.feature.appintro.AppIntroScreen
import com.dulpick.app.feature.auth.AuthScreen
import com.dulpick.app.feature.onboarding.couple.ARG_COUPLE_SHOWS_SKIP
import com.dulpick.app.feature.onboarding.couple.ARG_MY_NICKNAME
import com.dulpick.app.feature.onboarding.couple.CoupleScreen
import com.dulpick.app.feature.main.MainTabActions
import com.dulpick.app.feature.main.MainTabScreen
import com.dulpick.app.feature.mypage.connection.ConnectionManageScreen
import com.dulpick.app.feature.onboarding.datetype.ARG_DATETYPE_EDIT
import com.dulpick.app.feature.onboarding.datetype.DateTypeScreen
import com.dulpick.app.feature.onboarding.nickname.NicknameScreen
import com.dulpick.app.feature.pastdates.ARG_PASTDATES_HAS_CURRENT
import com.dulpick.app.feature.course.CourseDateScreen
import com.dulpick.app.feature.course.CourseEditScreen
import com.dulpick.app.feature.course.CourseReloadReason
import com.dulpick.app.feature.course.CoursePlacePickMode
import com.dulpick.app.feature.course.CoursePlacePickScreen
import com.dulpick.app.feature.course.PickedPlacesArg
import com.dulpick.app.ui.component.displayName
import com.dulpick.app.feature.course.CourseResultOrigin
import com.dulpick.app.feature.course.CourseResultScreen
import com.dulpick.app.feature.pastdates.PastDateCoursesScreen
import com.dulpick.app.feature.placeimport.PlaceImportScreen
import com.dulpick.app.feature.mapsearch.MapSearchScreen
import com.dulpick.app.feature.notice.NoticeArg
import com.dulpick.app.feature.notice.NoticeDetailScreen
import com.dulpick.app.feature.notice.NoticeListScreen
import com.dulpick.app.feature.placedetail.MapSearchReturnArg
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.dulpick.app.ui.component.AppButton
import com.dulpick.app.ui.component.AppButtonSize
import com.dulpick.app.ui.component.AppButtonVariant
import com.dulpick.app.ui.theme.Colors
import com.dulpick.app.ui.theme.Typography

@Composable
fun DulpickRoot(
    importUrl: StateFlow<String?> = MutableStateFlow(null),
    onImportUrlConsumed: () -> Unit = {},
    viewModel: RootViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sharedUrl by importUrl.collectAsStateWithLifecycle()

    when (val current = state) {
        RootState.Loading -> SplashScreen()
        RootState.Error -> ErrorScreen(onRetry = viewModel::retry)
        is RootState.Ready -> DulpickNavHost(
            startRoute = current.start.route,
            sharedUrl = sharedUrl,
            onSharedUrlConsumed = onImportUrlConsumed,
        )
    }
}

@Composable
private fun DulpickNavHost(
    startRoute: String,
    sharedUrl: String?,
    onSharedUrlConsumed: () -> Unit,
) {
    val navController = rememberNavController()
    // 공유로 받은 URL 을 대기열에 담는다. 로그인(MAIN) 상태가 되면 추출 모달을 띄운다
    var pendingImportUrl by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(sharedUrl) {
        val url = sharedUrl ?: return@LaunchedEffect
        pendingImportUrl = url
        // 소스는 비운다. 대기는 여기서 관리한다
        onSharedUrlConsumed()
    }

    // 현재 목적지가 로그인 후(MAIN 계열)인지. 로그인 전이면 URL 을 들고 기다린다
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route.orEmpty()
    val isLoggedIn = currentRoute == RootRoute.MAIN.route ||
        currentRoute.startsWith("${RootRoute.MAIN.route}/")

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

    // 로그인 상태에서만, 탭바 포함 전 화면 위에 Dialog 로 추출 모달을 띄운다
    val importUrl = pendingImportUrl
    if (importUrl != null && isLoggedIn) {
        PlaceImportScreen(
            sourceUrl = importUrl,
            onClose = { pendingImportUrl = null },
            onSessionExpired = {
                pendingImportUrl = null
                navController.navigateToAuth()
            },
        )
    }
}

// 로그인 후: 메인 탭 + 탭 밖 전체화면 상세(연결 관리·데이트 유형) 라우트
private fun NavGraphBuilder.mainRoutes(navController: NavHostController) {
    composable(RootRoute.MAIN.route) { entry ->
        // 지도 검색(push)에서 결과를 이 자리로 되돌려준다. 지도가 읽어 검색 결과 모드에 들어간다
        val pendingSearchJson by entry.savedStateHandle
            .getStateFlow<String?>(KEY_MAP_SEARCH, null)
            .collectAsStateWithLifecycle()
        MainTabScreen(
            pendingMapSearchArg = pendingSearchJson,
            onMapSearchConsumed = { entry.savedStateHandle[KEY_MAP_SEARCH] = null },
            // 검색 결과 모드에서 검색바 뒤로 → 같은 검색어로 지도 검색을 다시 연다
            onReopenMapSearch = { query ->
                navController.navigate("$MAIN_MAP_SEARCH_ROUTE?query=${Uri.encode(query)}")
            },
            // 로그아웃·세션 만료 → 백스택 전체를 비우고 로그인으로
            onLoggedOut = {
                navController.navigate(RootRoute.AUTH.route) {
                    popUpTo(navController.graph.id) { inclusive = true }
                }
            },
            // 상세는 탭 밖에서 전체화면 push
            actions = MainTabActions(
                onOpenDateType = { navController.navigate(MAIN_DATETYPE_ROUTE) },
                onOpenConnection = { navController.navigate(MAIN_CONNECTION_ROUTE) },
                // 마이페이지에서 연결 → 완료 시 연결 관리로
                onOpenCoupleConnect = { nickname ->
                    navController.navigate("$MAIN_COUPLE_ROUTE_BASE/${Uri.encode(nickname)}/$COUPLE_ORIGIN_MYPAGE")
                },
                // 홈에서 연결 → 완료 시 홈으로 되돌아온다(홈은 재진입 onAppear 로 갱신)
                onOpenCoupleConnectFromHome = { nickname ->
                    navController.navigate("$MAIN_COUPLE_ROUTE_BASE/${Uri.encode(nickname)}/$COUPLE_ORIGIN_HOME")
                },
                onOpenMapSearch = { navController.navigate(MAIN_MAP_SEARCH_ROUTE) },
                onOpenCourse = { dateCourseId ->
                    if (dateCourseId == null) {
                        navController.navigate(MAIN_COURSE_DATE_ROUTE)
                    } else {
                        navController.navigate(courseResultRoute(dateCourseId))
                    }
                },
                onOpenPastCourse = { dateCourseId ->
                    navController.navigate(courseResultRoute(dateCourseId, isPast = true))
                },
                onOpenNotice = { navController.navigate(MAIN_NOTICE_ROUTE) },
                onOpenPastDates = { hasCurrentCourse ->
                    navController.navigate("$MAIN_PASTDATES_ROUTE_BASE/$hasCurrentCourse")
                },
            ),
        )
    }
    mapSearchRoute(navController)
    coupleConnectRoute(navController)
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
    pastDatesRoute(navController)
    courseDateRoute(navController)
    coursePlacePickRoute(navController)
    courseResultRoute(navController)
    courseEditRoute(navController)
    noticeRoutes(navController)
    coursePlaceAddRoute(navController)
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

// 미연결 상태에서 커플 연결 플로우(건너뛰기 없음). origin 으로 완료 후 목적지를 가른다
private fun NavGraphBuilder.coupleConnectRoute(navController: NavHostController) {
    composable(
        route = "$MAIN_COUPLE_ROUTE_BASE/{$ARG_MY_NICKNAME}/{$ARG_COUPLE_ORIGIN}",
        arguments = listOf(
            navArgument(ARG_MY_NICKNAME) { type = NavType.StringType },
            navArgument(ARG_COUPLE_ORIGIN) { type = NavType.StringType },
            navArgument(ARG_COUPLE_SHOWS_SKIP) {
                type = NavType.BoolType
                defaultValue = false
            },
        ),
        enterTransition = { slideInHorizontally { it } },
        exitTransition = { slideOutHorizontally { -it / PARALLAX_DIVISOR } },
        popEnterTransition = { slideInHorizontally { -it / PARALLAX_DIVISOR } },
        popExitTransition = { slideOutHorizontally { it } },
    ) { entry ->
        val fromHome = entry.arguments?.getString(ARG_COUPLE_ORIGIN) == COUPLE_ORIGIN_HOME
        CoupleScreen(
            onBack = { navController.popBackStack() },
            onFinished = {
                if (fromHome) {
                    // 홈에서 왔으면 홈으로 되돌아간다. 홈은 재진입 onAppear 에서 스스로 갱신한다
                    navController.popBackStack()
                } else {
                    // 마이페이지에서 왔으면 커플 플로우를 걷어내고 연결 관리로 대체
                    navController.navigate(MAIN_CONNECTION_ROUTE) {
                        popUpTo("$MAIN_COUPLE_ROUTE_BASE/{$ARG_MY_NICKNAME}/{$ARG_COUPLE_ORIGIN}") {
                            inclusive = true
                        }
                    }
                }
            },
            onSessionExpired = { navController.navigateToAuth() },
        )
    }
}

// 홈 헤더 달력(연결 상태) → 지난 데이트 코스 목록
private fun NavGraphBuilder.pastDatesRoute(navController: NavHostController) {
    composable(
        route = "$MAIN_PASTDATES_ROUTE_BASE/{$ARG_PASTDATES_HAS_CURRENT}",
        arguments = listOf(
            navArgument(ARG_PASTDATES_HAS_CURRENT) {
                type = NavType.BoolType
                defaultValue = false
            },
        ),
        enterTransition = { slideInHorizontally { it } },
        exitTransition = { slideOutHorizontally { -it / PARALLAX_DIVISOR } },
        popEnterTransition = { slideInHorizontally { -it / PARALLAX_DIVISOR } },
        popExitTransition = { slideOutHorizontally { it } },
    ) {
        PastDateCoursesScreen(
            onBack = { navController.popBackStack() },
            onOpenCourseFlow = { navController.navigate(MAIN_COURSE_DATE_ROUTE) },
            onOpenCourseResult = { id ->
                navController.navigate(courseResultRoute(id, isPast = true))
            },
        )
    }
}

// 데이트 코스 날짜 선택(push). 다음 화면(장소 고르기)은 아직 없어 날짜까지만 간다
private fun NavGraphBuilder.courseDateRoute(navController: NavHostController) {
    composable(
        route = MAIN_COURSE_DATE_ROUTE,
        enterTransition = { slideInHorizontally { it } },
        exitTransition = { slideOutHorizontally { -it / PARALLAX_DIVISOR } },
        popEnterTransition = { slideInHorizontally { -it / PARALLAX_DIVISOR } },
        popExitTransition = { slideOutHorizontally { it } },
    ) {
        CourseDateScreen(
            onBack = { navController.popBackStack() },
            onSessionExpired = { navController.navigateToAuth() },
            onPlacePick = { dateCourseId, _ ->
                navController.navigate("$MAIN_COURSE_PLACE_ROUTE_BASE/$dateCourseId")
            },
        )
    }
}

// 코스에 담을 장소 고르기(push). 확정 저장까지 여기서 한다
private fun NavGraphBuilder.coursePlacePickRoute(navController: NavHostController) {
    composable(
        route = "$MAIN_COURSE_PLACE_ROUTE_BASE/{$ARG_DATE_COURSE_ID}",
        arguments = listOf(navArgument(ARG_DATE_COURSE_ID) { type = NavType.StringType }),
        enterTransition = { slideInHorizontally { it } },
        exitTransition = { slideOutHorizontally { -it / PARALLAX_DIVISOR } },
        popEnterTransition = { slideInHorizontally { -it / PARALLAX_DIVISOR } },
        popExitTransition = { slideOutHorizontally { it } },
    ) { entry ->
        CoursePlacePickScreen(
            dateCourseId = entry.arguments?.getString(ARG_DATE_COURSE_ID).orEmpty(),
            onBack = { navController.popBackStack() },
            onSessionExpired = { navController.navigateToAuth() },
            onBuilt = { id ->
                // 결과에서 뒤로 가면 코스 흐름을 닫는다. 장소 고르기로 돌아가지 않는다
                navController.navigate(courseResultRoute(id)) {
                    popUpTo(MAIN_COURSE_DATE_ROUTE) { inclusive = true }
                }
            },
        )
    }
}

// 코스 결과로 가는 길. 지난 데이트로 들어가면 결과 화면이 수정·알리기를 숨긴다
private fun courseResultRoute(dateCourseId: String, isPast: Boolean = false): String {
    val origin = if (isPast) COURSE_ORIGIN_PAST else COURSE_ORIGIN_BUILT
    return "$MAIN_COURSE_RESULT_ROUTE_BASE/$dateCourseId/$origin"
}

// 확정된 코스 보기(push). 지난 데이트 목록에서도 같은 화면을 쓴다
private fun NavGraphBuilder.courseResultRoute(navController: NavHostController) {
    composable(
        route = "$MAIN_COURSE_RESULT_ROUTE_BASE/{$ARG_DATE_COURSE_ID}/{$ARG_COURSE_ORIGIN}",
        arguments = listOf(
            navArgument(ARG_DATE_COURSE_ID) { type = NavType.StringType },
            navArgument(ARG_COURSE_ORIGIN) { type = NavType.StringType },
        ),
        enterTransition = { slideInHorizontally { it } },
        exitTransition = { slideOutHorizontally { -it / PARALLAX_DIVISOR } },
        popEnterTransition = { slideInHorizontally { -it / PARALLAX_DIVISOR } },
        popExitTransition = { slideOutHorizontally { it } },
    ) { entry ->
        // 수정 화면이 저장·충돌 뒤 이 자리에 신호를 남긴다
        val reloadReason by entry.savedStateHandle
            .getStateFlow<String?>(KEY_COURSE_RELOAD, null)
            .collectAsStateWithLifecycle()
        CourseResultScreen(
            dateCourseId = entry.arguments?.getString(ARG_DATE_COURSE_ID).orEmpty(),
            reloadReason = reloadReason?.let(CourseReloadReason::valueOf),
            onReloadConsumed = { entry.savedStateHandle[KEY_COURSE_RELOAD] = null },
            origin = if (entry.arguments?.getString(ARG_COURSE_ORIGIN) == COURSE_ORIGIN_PAST) {
                CourseResultOrigin.PAST_DATE
            } else {
                CourseResultOrigin.COURSE_BUILT
            },
            onBack = { navController.popBackStack() },
            onSessionExpired = { navController.navigateToAuth() },
            onEdit = { id -> navController.navigate("$MAIN_COURSE_EDIT_ROUTE_BASE/$id") },
        )
    }
}

// 공지사항 목록·상세(push). 상세 조회 API 가 없어 목록에서 받은 값을 그대로 넘긴다
private fun NavGraphBuilder.noticeRoutes(navController: NavHostController) {
    composable(
        route = MAIN_NOTICE_ROUTE,
        enterTransition = { slideInHorizontally { it } },
        exitTransition = { slideOutHorizontally { -it / PARALLAX_DIVISOR } },
        popEnterTransition = { slideInHorizontally { -it / PARALLAX_DIVISOR } },
        popExitTransition = { slideOutHorizontally { it } },
    ) {
        NoticeListScreen(
            onBack = { navController.popBackStack() },
            onOpenNotice = { notice ->
                navController.navigate(
                    "$MAIN_NOTICE_DETAIL_ROUTE_BASE/${Uri.encode(NoticeArg.from(notice).encode())}",
                )
            },
        )
    }
    composable(
        route = "$MAIN_NOTICE_DETAIL_ROUTE_BASE/{$ARG_NOTICE}",
        arguments = listOf(navArgument(ARG_NOTICE) { type = NavType.StringType }),
        enterTransition = { slideInHorizontally { it } },
        exitTransition = { slideOutHorizontally { -it / PARALLAX_DIVISOR } },
        popEnterTransition = { slideInHorizontally { -it / PARALLAX_DIVISOR } },
        popExitTransition = { slideOutHorizontally { it } },
    ) { entry ->
        val notice = entry.arguments?.getString(ARG_NOTICE)
            ?.let(NoticeArg::decode)
            ?.toNotice()
        // 인자를 못 읽으면 보여줄 게 없다. 목록으로 되돌린다
        if (notice == null) {
            LaunchedEffect(Unit) { navController.popBackStack() }
        } else {
            NoticeDetailScreen(notice = notice, onBack = { navController.popBackStack() })
        }
    }
}

// 코스 수정 중 장소 더하기(push). 같은 장소 고르기 화면을 더하기 모드로 쓴다.
// 이미 담긴 장소는 빼고 보여주고, 고른 것만 수정 화면으로 돌려준다
private fun NavGraphBuilder.coursePlaceAddRoute(navController: NavHostController) {
    composable(
        route = "$MAIN_COURSE_PLACE_ADD_ROUTE?$ARG_EXCLUDING={$ARG_EXCLUDING}",
        arguments = listOf(
            navArgument(ARG_EXCLUDING) {
                type = NavType.StringType
                defaultValue = ""
            },
        ),
        enterTransition = { slideInHorizontally { it } },
        exitTransition = { slideOutHorizontally { -it / PARALLAX_DIVISOR } },
        popEnterTransition = { slideInHorizontally { -it / PARALLAX_DIVISOR } },
        popExitTransition = { slideOutHorizontally { it } },
    ) { entry ->
        CoursePlacePickScreen(
            onBack = { navController.popBackStack() },
            onSessionExpired = { navController.navigateToAuth() },
            mode = CoursePlacePickMode.ADD,
            excluding = entry.arguments?.getString(ARG_EXCLUDING)
                .orEmpty()
                .split(",")
                .filter { it.isNotBlank() },
            onPicked = { picked ->
                val arg = PickedPlacesArg.from(picked) { it.place.category.displayName() }
                navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.set(KEY_COURSE_PICKED, arg.encode())
                navController.popBackStack()
            },
        )
    }
}

// 코스 수정(push). 저장하면 결과 화면이 다시 읽게 신호를 남기고 돌아간다
private fun NavGraphBuilder.courseEditRoute(navController: NavHostController) {
    composable(
        route = "$MAIN_COURSE_EDIT_ROUTE_BASE/{$ARG_DATE_COURSE_ID}",
        arguments = listOf(navArgument(ARG_DATE_COURSE_ID) { type = NavType.StringType }),
        enterTransition = { slideInHorizontally { it } },
        exitTransition = { slideOutHorizontally { -it / PARALLAX_DIVISOR } },
        popEnterTransition = { slideInHorizontally { -it / PARALLAX_DIVISOR } },
        popExitTransition = { slideOutHorizontally { it } },
    ) { entry ->
        // 장소 추가 화면이 고른 장소를 이 자리에 남긴다
        val picked by entry.savedStateHandle
            .getStateFlow<String?>(KEY_COURSE_PICKED, null)
            .collectAsStateWithLifecycle()
        CourseEditScreen(
            dateCourseId = entry.arguments?.getString(ARG_DATE_COURSE_ID).orEmpty(),
            onBack = { navController.popBackStack() },
            onSessionExpired = { navController.navigateToAuth() },
            // 저장도 충돌도 결과 화면이 서버에서 다시 읽는다. 충돌은 그 사실을 알린다
            onSaved = { navController.popBackWithCourseReload(CourseReloadReason.SAVED) },
            onConflicted = { navController.popBackWithCourseReload(CourseReloadReason.CONFLICT) },
            onAddPlace = { excluding ->
                navController.navigate(
                    "$MAIN_COURSE_PLACE_ADD_ROUTE?$ARG_EXCLUDING=${Uri.encode(excluding.joinToString(","))}",
                )
            },
            pickedPlaces = picked,
            onPickedConsumed = { entry.savedStateHandle[KEY_COURSE_PICKED] = null },
        )
    }
}

// 결과 화면에 다시 읽으라고 남기고 돌아간다
private fun NavHostController.popBackWithCourseReload(reason: CourseReloadReason) {
    previousBackStackEntry?.savedStateHandle?.set(KEY_COURSE_RELOAD, reason.name)
    popBackStack()
}

// 지도 전용 장소 검색(push). 결과 제출/행탭 시 검색을 pop 하고 지도(MAIN)를 검색 결과 모드로 만든다.
// query 를 넘기면(검색바 뒤로로 재진입) 그 검색어로 곧장 검색해 결과를 복원한다
private fun NavGraphBuilder.mapSearchRoute(navController: NavHostController) {
    composable(
        route = "$MAIN_MAP_SEARCH_ROUTE?query={$ARG_MAP_SEARCH_QUERY}",
        arguments = listOf(
            navArgument(ARG_MAP_SEARCH_QUERY) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
        ),
        enterTransition = { slideInHorizontally { it } },
        exitTransition = { slideOutHorizontally { -it / PARALLAX_DIVISOR } },
        popEnterTransition = { slideInHorizontally { -it / PARALLAX_DIVISOR } },
        popExitTransition = { slideOutHorizontally { it } },
    ) { entry ->
        MapSearchScreen(
            initialQuery = entry.arguments?.getString(ARG_MAP_SEARCH_QUERY),
            onBack = { navController.popBackStack() },
            // 검색 제출 → 전체 결과를 지도 검색 결과 모드로 (iOS searchConfirmed 대응)
            onSearchConfirmed = { query, places ->
                navController.returnSearchResult(MapSearchReturnArg.confirming(query, places))
            },
            // 결과 행 탭 → 그 장소만 남기고 상세를 연다 (iOS placeSelected 대응)
            onPlaceSelected = { query, place ->
                navController.returnSearchResult(MapSearchReturnArg.selecting(place, query))
            },
            onSessionExpired = { navController.navigateToAuth() },
        )
    }
}

// 검색 결과를 MAIN 의 savedStateHandle 로 되돌리고 검색 화면을 pop 한다
private fun NavHostController.returnSearchResult(arg: MapSearchReturnArg) {
    previousBackStackEntry?.savedStateHandle?.set(KEY_MAP_SEARCH, arg.encode())
    popBackStack()
}

// 지도 검색 결과 → 지도(MAIN)로 되돌려줄 때 쓰는 savedStateHandle 키
private const val KEY_MAP_SEARCH = "map_search"

// 코스 수정 → 결과 화면에 다시 읽으라고 남기는 키
private const val KEY_COURSE_RELOAD = "course_reload"

// 장소 추가 → 코스 수정 화면에 고른 장소를 남기는 키
private const val KEY_COURSE_PICKED = "course_picked"

// 마이페이지에서 여는 전체화면 라우트 (탭 밖 push)
private const val MAIN_DATETYPE_ROUTE = "main/datetype"
private const val MAIN_CONNECTION_ROUTE = "main/connection"
private const val MAIN_COUPLE_ROUTE_BASE = "main/couple"
private const val MAIN_MAP_SEARCH_ROUTE = "main/map-search"
// 검색바 뒤로로 재진입할 때 넘기는 검색어(선택 인자). 없으면 최근 검색어 화면
private const val ARG_MAP_SEARCH_QUERY = "query"
private const val MAIN_PASTDATES_ROUTE_BASE = "main/past-dates"
private const val MAIN_COURSE_DATE_ROUTE = "main/course/date"
private const val MAIN_COURSE_PLACE_ROUTE_BASE = "main/course/places"
private const val MAIN_COURSE_RESULT_ROUTE_BASE = "main/course/result"
private const val MAIN_COURSE_EDIT_ROUTE_BASE = "main/course/edit"
private const val MAIN_COURSE_PLACE_ADD_ROUTE = "main/course/places/add"
private const val MAIN_NOTICE_ROUTE = "main/notices"
private const val MAIN_NOTICE_DETAIL_ROUTE_BASE = "main/notices/detail"
// 상세로 넘기는 공지 한 건(JSON)
private const val ARG_NOTICE = "notice"
// 이미 코스에 담긴 장소 번호를 쉼표로 이어 넘긴다
private const val ARG_EXCLUDING = "excluding"
private const val ARG_DATE_COURSE_ID = "dateCourseId"

// 코스 결과 진입 출처. 지난 데이트면 수정·알리기를 숨긴다
private const val ARG_COURSE_ORIGIN = "courseOrigin"
private const val COURSE_ORIGIN_BUILT = "built"
private const val COURSE_ORIGIN_PAST = "past"

// 커플 연결 진입 출처. 완료 후 홈으로 되돌아갈지 연결 관리로 갈지 가른다
private const val ARG_COUPLE_ORIGIN = "origin"
private const val COUPLE_ORIGIN_HOME = "home"
private const val COUPLE_ORIGIN_MYPAGE = "mypage"
// 스플래시 로고 묶음 크기와 위치 (iOS SplashMetric 과 동일)
private val SPLASH_BUNDLE_WIDTH = 201.dp
private val SPLASH_BUNDLE_HEIGHT = 164.dp
private val SPLASH_BUNDLE_OFFSET = 36.dp

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
    Box(modifier = Modifier.fillMaxSize()) {
        // 배경은 브랜드 색 위에 곡선 장식이 얹힌 한 장이다. 시안 비율(393 × 852)과 화면 비율이
        // 달라 잘라서 채운다 (iOS scaledToFill 대응)
        Image(
            painter = painterResource(R.drawable.splashbackground),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        // 로고와 문구를 한 장으로 내보낸 그림. 시안(393 × 852)에서 묶음 중심이 화면 중심보다 36 위다
        // (iOS SplashView·SplashMetric 대응)
        Image(
            painter = painterResource(R.drawable.splashbundle),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = -SPLASH_BUNDLE_OFFSET)
                .size(width = SPLASH_BUNDLE_WIDTH, height = SPLASH_BUNDLE_HEIGHT),
        )
    }
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
