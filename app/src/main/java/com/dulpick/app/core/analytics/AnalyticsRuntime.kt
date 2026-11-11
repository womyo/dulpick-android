package com.dulpick.app.core.analytics

import com.mixpanel.android.mpmetrics.MixpanelAPI

// 믹스패널이 초기화됐는지를 이 묶음 안에서만 나눠 본다.
//
// 켜짐 여부를 직접 들고 있어야 한다. SDK 는 초기화 전에 인스턴스를 부르면
// 빈 토큰으로 이벤트를 쌓는 인스턴스를 만들어 버린다 (iOS AnalyticsRuntime 대응)
internal object AnalyticsRuntime {
    @Volatile
    var mixpanel: MixpanelAPI? = null
}
