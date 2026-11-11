package com.dulpick.app.feature.main

import androidx.lifecycle.ViewModel
import com.dulpick.app.core.analytics.Analytics
import com.dulpick.app.core.analytics.AnalyticsEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

// 탭 전환 이벤트만 담당한다 (iOS MainTabFeature 의 tabSelected 대응)
@HiltViewModel
class MainTabViewModel @Inject constructor(
    private val analytics: Analytics,
) : ViewModel() {

    // 탭바에서 탭을 고를 때 부른다. 홈은 iOS 와 같이 이벤트가 없다
    fun trackTabSelected(route: String) {
        val event = when (route) {
            MainTab.EXPLORE.route -> AnalyticsEvent.ExploreViewed
            MainTab.MAP.route -> AnalyticsEvent.MapViewed
            MainTab.MY.route -> AnalyticsEvent.MyPageViewed
            else -> return
        }
        analytics.track(event)
    }
}
