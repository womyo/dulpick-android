package com.dulpick.app.core.notification

import com.dulpick.app.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

// FCM 수신 지점. 토큰 변경과 앱이 떠 있을 때 온 메시지를 받는다
@AndroidEntryPoint
class DulpickMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var tokenStore: PushTokenStore

    // 토큰은 재설치·데이터 삭제·앱 복원 시 바뀐다. 흘려 두면 등록 구독이 서버에 다시 올린다
    override fun onNewToken(token: String) {
        tokenStore.update(token)
    }

    // notification 묶음이 실린 메시지는 백그라운드에서 시스템이 띄우고, 포그라운드에서는 여기로만 온다
    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data[KEY_TITLE]
        val body = message.notification?.body ?: message.data[KEY_BODY]
        if (title == null && body == null) return
        PushNotifier.show(
            context = this,
            title = title ?: getString(R.string.app_name),
            body = body.orEmpty(),
        )
    }

    private companion object {
        const val KEY_TITLE = "title"
        const val KEY_BODY = "body"
    }
}
