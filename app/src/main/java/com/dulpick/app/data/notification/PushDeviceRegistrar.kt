package com.dulpick.app.data.notification

import android.util.Log
import com.dulpick.app.core.coroutine.ApplicationScope
import com.dulpick.app.core.notification.PushTokenStore
import com.dulpick.app.domain.notification.PushDeviceRepository
import com.dulpick.app.domain.notification.PushError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
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

    // 등록 실패는 화면에 알리지 않는다.
    //
    // 토큰은 StateFlow 라 같은 값을 다시 넣어도 흐르지 않는다. 그래서 연결·서버 문제로 실패하면
    // 여기서 짧게 다시 시도해야 하고, 그마저 안 되면 다음 토큰이나 다음 실행까지 알림이 오지 않는다
    private suspend fun register(token: String) {
        var attempt = 0
        while (true) {
            val error = runRegister(token) ?: return
            if (!isRetryable(error) || attempt >= RETRY_DELAYS_MS.size) {
                giveUp(error)
                return
            }
            Log.i(LOG_TAG, "푸시 기기 등록을 ${RETRY_DELAYS_MS[attempt]}ms 뒤 다시 시도한다")
            delay(RETRY_DELAYS_MS[attempt])
            attempt++
        }
    }

    // 성공하면 null, 실패하면 그 오류를 낸다. 취소는 오류가 아니라 그대로 올린다
    private suspend fun runRegister(token: String): Throwable? =
        try {
            pushDevice.registerDevice(token)
            null
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            error
        }

    // 세션이 없거나(401) 본문이 거부된 것(400)은 다시 보내도 같은 결과다
    private fun isRetryable(error: Throwable): Boolean =
        error != PushError.Unauthorized && error != PushError.InvalidRequest

    private fun giveUp(error: Throwable) {
        // 재발급까지 실패한 401 이면 세션 없이 계속 두드리지 않게 구독을 끊는다.
        // 다음 로그인·세션 복구가 다시 start() 한다 (iOS 가 로그인 화면으로 물러날 때 구독을 끊는 것과 같다)
        if (error == PushError.Unauthorized) {
            Log.i(LOG_TAG, "세션이 없어 푸시 기기 등록 구독을 끊는다")
            stop()
        } else {
            Log.w(LOG_TAG, "푸시 기기 등록 실패", error)
        }
    }

    private companion object {
        const val LOG_TAG = "PushDevice"

        // 일시적 실패에 쓰는 재시도 간격. 다 쓰면 포기하고 다음 토큰·다음 실행을 기다린다
        val RETRY_DELAYS_MS = listOf(2_000L, 8_000L)
    }
}
