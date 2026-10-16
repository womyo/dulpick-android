package com.dulpick.app.data.notification.local

import com.dulpick.app.core.storage.SecureStorage
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

// deviceId 는 서버가 발급하지 않는다. 앱이 만들어 보관한다.
// 세션 키와 다른 키라 로그아웃해도 남아, 다시 로그인해도 같은 기기로 등록된다
@Singleton
class PushLocalDataSource @Inject constructor(
    private val storage: SecureStorage,
) {
    // 처음 부르는 쪽이 둘이면 서로 다른 값을 만들어 저장할 수 있어 생성을 직렬화한다
    private val mutex = Mutex()

    suspend fun deviceId(): String = mutex.withLock {
        storage.getString(KEY) ?: UUID.randomUUID().toString().also { storage.putString(KEY, it) }
    }

    private companion object {
        const val KEY = "push-device-id"
    }
}
