package com.dulpick.app.core.analytics

import android.util.Log
import org.json.JSONObject
import java.time.Instant
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

// 화면·뷰모델이 이벤트를 보낼 때 쓰는 창구.
// 초기화가 안 됐으면 아무 일도 하지 않는다 (iOS AnalyticsEventSender + AnalyticsIdentity 대응)
@Singleton
class Analytics @Inject constructor() {

    // 마지막으로 묶은 사람. user_id 를 속성으로 싣는 이벤트가 있어 들고 있는다
    @Volatile
    var userId: String? = null
        private set

    fun track(event: AnalyticsEvent) {
        val mixpanel = AnalyticsRuntime.mixpanel ?: return skip("이벤트", event.name)
        mixpanel.track(event.name, event.properties.toJson())
    }

    // 로그인이 성공한 때와 앱을 다시 켜서 세션이 복구된 때 부른다
    fun identify(userId: String) {
        this.userId = userId
        val mixpanel = AnalyticsRuntime.mixpanel ?: return skip("사람 묶기")
        mixpanel.identify(userId)
    }

    // 가입일을 한 번만 적는다. 이미 적힌 값이 있으면 덮어쓰지 않는다
    fun markSignedUp(at: Instant) {
        val mixpanel = AnalyticsRuntime.mixpanel ?: return skip("가입일 적기")
        mixpanel.people.setOnce(JSONObject().put(CREATED_PROPERTY, DateTimeFormatter.ISO_INSTANT.format(at)))
    }

    // 로그아웃·탈퇴·세션 만료에서 부른다. SDK 가 끊기 전에 쌓인 이벤트를 보낸다
    fun reset() {
        userId = null
        val mixpanel = AnalyticsRuntime.mixpanel ?: return skip("묶음 끊기")
        mixpanel.flush()
        mixpanel.reset()
    }

    private fun Map<String, Any>.toJson(): JSONObject =
        JSONObject().also { json -> forEach { (key, value) -> json.put(key, value) } }

    private fun skip(what: String, name: String? = null) {
        Log.i(LOG_TAG, "믹스패널이 초기화되지 않아 ${what}를 건너뛴다${name?.let { " name=$it" }.orEmpty()}")
    }

    private companion object {
        // 믹스패널이 가입일로 읽는 예약 속성
        const val CREATED_PROPERTY = "\$created"
        const val LOG_TAG = "Analytics"
    }
}
