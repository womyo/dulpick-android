package com.dulpick.app.core.analytics

import android.content.Context
import android.util.Log
import com.microsoft.clarity.Clarity
import com.microsoft.clarity.ClarityConfig
import com.mixpanel.android.mpmetrics.MixpanelAPI

// 클라리티 세션 녹화와 믹스패널 이벤트 초기화. DulpickApp 이 시작 때 한 번 부른다.
// SDK 를 아는 것은 core/analytics 뿐이다 (iOS AnalyticsBootstrap 대응)
object AnalyticsInitializer {

    // 키가 비면 그 SDK 만 건너뛴다. 디버그 빌드에서 분석을 끄는 방법이기도 하다
    fun init(context: Context, clarityProjectId: String, mixpanelToken: String) {
        startClarity(context, clarityProjectId)
        startMixpanel(context, mixpanelToken)
    }

    // 메인 스레드에서, Application.onCreate 안에서 불러야 한다 (SDK 요구사항)
    private fun startClarity(context: Context, projectId: String) {
        if (projectId.isBlank()) {
            Log.i(LOG_TAG, "클라리티 프로젝트 ID 가 비어 초기화를 건너뛴다")
            return
        }
        val started = Clarity.initialize(context, ClarityConfig(projectId = projectId))
        if (!started) Log.e(LOG_TAG, "클라리티 초기화에 실패했다")
    }

    private fun startMixpanel(context: Context, token: String) {
        if (token.isBlank()) {
            Log.i(LOG_TAG, "믹스패널 토큰이 비어 초기화를 건너뛴다")
            return
        }
        AnalyticsRuntime.mixpanel = MixpanelAPI.getInstance(context, token, TRACK_AUTOMATIC_EVENTS)
    }

    // iOS 와 같이 SDK 기본 이벤트(앱 설치·업데이트 등)도 함께 받는다
    private const val TRACK_AUTOMATIC_EVENTS = true
    private const val LOG_TAG = "Analytics"
}
