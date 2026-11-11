package com.dulpick.app.feature.root

import com.dulpick.app.core.analytics.AnalyticsScreenName

// 라우트 → 대시보드 화면 이름. iOS 는 TCA 상태에서 현재 화면을 계산하고,
// 안드로이드는 NavHost 의 현재 목적지를 같은 이름으로 옮긴다 (iOS AnalyticsScreenName 대응).
//
// 지도 위 시트(장소·게시글 상세, 별칭)와 마이페이지 시트는 라우트가 없어 아직 여기 없다

// 루트 NavHost 의 목적지. 메인 탭 자체는 탭 NavHost 가 알려주므로 null 이다
internal fun rootScreenName(route: String): AnalyticsScreenName? {
    // 첫 프레임엔 목적지가 없다. 그때 보이는 건 스플래시다
    if (route.isEmpty()) return AnalyticsScreenName.SPLASH
    SCREEN_BY_ROUTE[route]?.let { return it }
    return SCREEN_BY_PREFIX.firstOrNull { (prefix, _) -> route.startsWith(prefix) }?.second
}

// 인자가 없는 라우트
private val SCREEN_BY_ROUTE = mapOf(
    RootRoute.APP_INTRO.route to AnalyticsScreenName.APP_INTRO,
    RootRoute.AUTH.route to AnalyticsScreenName.AUTH,
    RootRoute.ONBOARDING.route to AnalyticsScreenName.NICKNAME,
    RootRoute.DATETYPE.route to AnalyticsScreenName.DATE_TYPE,
    MAIN_DATETYPE_ROUTE to AnalyticsScreenName.DATE_TYPE,
    MAIN_CONNECTION_ROUTE to AnalyticsScreenName.CONNECTION_MANAGE,
    MAIN_MAP_SEARCH_ROUTE to AnalyticsScreenName.PLACE_SEARCH,
    MAIN_COURSE_DATE_ROUTE to AnalyticsScreenName.COURSE_DATE,
    MAIN_NOTICE_ROUTE to AnalyticsScreenName.NOTICE_LIST,
)

// 인자가 붙는 라우트. 더 긴 접두사를 앞에 둔다 — 짧은 쪽이 먼저 걸리면 엉뚱한 이름이 남는다
private val SCREEN_BY_PREFIX = listOf(
    MAIN_COURSE_PLACE_ADD_ROUTE to AnalyticsScreenName.COURSE_PLACE_PICK,
    MAIN_COURSE_PLACE_ROUTE_BASE to AnalyticsScreenName.COURSE_PLACE_PICK,
    MAIN_COURSE_RESULT_ROUTE_BASE to AnalyticsScreenName.COURSE_RESULT,
    MAIN_COURSE_EDIT_ROUTE_BASE to AnalyticsScreenName.COURSE_EDIT,
    MAIN_NOTICE_DETAIL_ROUTE_BASE to AnalyticsScreenName.NOTICE_DETAIL,
    MAIN_PASTDATES_ROUTE_BASE to AnalyticsScreenName.PAST_DATE_COURSES,
    MAIN_MAP_SEARCH_ROUTE to AnalyticsScreenName.PLACE_SEARCH,
    MAIN_COUPLE_ROUTE_BASE to AnalyticsScreenName.COUPLE_CONNECT,
    ROUTE_COUPLE_BASE to AnalyticsScreenName.COUPLE_CONNECT,
)
