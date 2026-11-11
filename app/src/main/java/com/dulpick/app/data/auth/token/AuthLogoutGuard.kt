package com.dulpick.app.data.auth.token

import javax.inject.Inject
import javax.inject.Singleton

// 서버 로그아웃이 보낼 리프레시 토큰이 그사이 회전되지 않게 한다.
// 로그아웃 중에 다른 요청이 401 을 받으면 재발급하지 않고 그대로 실패시킨다
@Singleton
class AuthLogoutGuard @Inject constructor() {
    @Volatile
    var isLoggingOut: Boolean = false
        private set

    suspend fun <T> whileLoggingOut(block: suspend () -> T): T {
        isLoggingOut = true
        try {
            return block()
        } finally {
            isLoggingOut = false
        }
    }
}
