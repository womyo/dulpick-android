package com.dulpick.app.core.notification

import android.content.Context

// 알림 채널을 미리 만든다. DulpickApp 이 호출한다 (iOS NotificationBootstrap 대응).
// Firebase 자체는 google-services.json 이 있으면 SDK 가 앱 시작 시 스스로 초기화한다
object PushNotificationInitializer {
    fun init(context: Context) {
        PushNotifier.ensureChannel(context)
    }
}
