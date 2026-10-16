package com.dulpick.app.data.notification

import android.util.Log
import com.dulpick.app.core.coroutine.ApplicationScope
import com.dulpick.app.core.notification.PushTokenStore
import com.dulpick.app.domain.notification.PushDeviceRepository
import com.dulpick.app.domain.notification.PushError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

// FCM 토큰이 올 때마다 기기를 등록한다 (iOS RootFlow 의 registerPushDevice 대응).
// 구독하면 마지막 토큰을 먼저 받으므로 로그인 직후와 앱 재실행 직후에도 한 번 등록된다
@Singleton
@Suppress("TooGenericExceptionCaught")
class PushDeviceRegistrar @Inject constructor(
    private val tokenStore: PushTokenStore,
    private val pushDevice: PushDeviceRepository,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private var job: Job? = null

    // 로그인·세션 복구 뒤에 부른다. 이미 돌고 있으면 그대로 둔다
    @Synchronized
    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            tokenStore.refresh()
            tokenStore.tokens.collect { token -> register(token) }
        }
    }

    // 로그아웃 뒤에 부른다. 세션 없이 등록을 시도하지 않게 구독을 끊는다
    @Synchronized
    fun stop() {
        job?.cancel()
        job = null
    }

    // 등록 실패는 화면에 알리지 않는다. 다음 토큰이나 다음 실행에서 다시 시도된다
    private suspend fun register(token: String) {
        try {
            pushDevice.registerDevice(token)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            // 재발급까지 실패한 401 이면 세션 없이 계속 두드리지 않게 구독을 끊는다.
            // 다음 로그인·세션 복구가 다시 start() 한다 (iOS 가 로그인 화면으로 물러날 때 구독을 끊는 것과 같다)
            if (error == PushError.Unauthorized) {
                Log.i(LOG_TAG, "세션이 없어 푸시 기기 등록 구독을 끊는다")
                stop()
            } else {
                Log.w(LOG_TAG, "푸시 기기 등록 실패", error)
            }
        }
    }

    private companion object {
        const val LOG_TAG = "PushDevice"
    }
}
