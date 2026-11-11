package com.dulpick.app.feature.main

import com.dulpick.app.core.analytics.AnalyticsScreenName

// 탭 NavHost 의 목적지 → 대시보드 화면 이름 (iOS MainTabFeature.currentScreenName 대응)
internal fun tabScreenName(route: String): AnalyticsScreenName? = when (route) {
    MainTab.HOME.route -> AnalyticsScreenName.HOME
    MainTab.EXPLORE.route -> AnalyticsScreenName.EXPLORE
    MainTab.MAP.route -> AnalyticsScreenName.MAP
    MainTab.MY.route -> AnalyticsScreenName.MY_PAGE
    EXPLORE_SEARCH_ROUTE -> AnalyticsScreenName.SEARCH
    else -> null
}
