package com.dulpick.app.core.notification

import android.content.Context
import android.util.Log
import com.dulpick.app.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import javax.inject.Inject
import javax.inject.Singleton

// FCM 등록 토큰을 들고 흘린다 (iOS RemoteNotificationClient.fcmTokenStream 대응).
// Firebase SDK 를 아는 것은 core/notification 뿐이다
@Singleton
class PushTokenStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val latest = MutableStateFlow<String?>(null)

    // 구독하면 마지막 토큰을 먼저 준다. 토큰이 바뀔 때마다 다시 흐른다
    val tokens: Flow<String> = latest.filterNotNull()

    fun update(token: String) {
        // 콘솔에서 테스트 메시지를 보낼 때 쓸 토큰. 릴리스에는 남기지 않는다
        if (BuildConfig.DEBUG) Log.i(LOG_TAG, "FCM 토큰 $token")
        latest.value = token
    }

    // onNewToken 은 토큰이 바뀔 때만 온다. 앱이 다시 떠서 그 호출이 없는 경우를 메운다
    fun refresh() {
        if (!isFirebaseConfigured()) {
            Log.i(LOG_TAG, "Firebase 설정이 없어 FCM 토큰 조회를 건너뛴다")
            return
        }
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { update(it) }
            .addOnFailureListener { error -> Log.w(LOG_TAG, "FCM 토큰 조회 실패", error) }
    }

    private fun isFirebaseConfigured(): Boolean = FirebaseApp.getApps(context).isNotEmpty()

    private companion object {
        const val LOG_TAG = "PushToken"
    }
}
