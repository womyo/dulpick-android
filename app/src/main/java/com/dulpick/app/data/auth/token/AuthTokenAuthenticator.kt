package com.dulpick.app.data.auth.token

import com.dulpick.app.data.auth.local.AuthLocalDataSource
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

// 동시 401 은 한 번만 갱신(single-flight)
class AuthTokenAuthenticator(
    private val authLocal: AuthLocalDataSource,
    private val refresher: AuthTokenRefresher,
) : Authenticator {

    private val lock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        // 이미 여러 번 재시도했으면 포기한다
        if (responseCount(response) >= MAX_RETRY) return null

        val failedToken = response.request.header("Authorization")?.removePrefix("Bearer ")

        synchronized(lock) {
            val stored = runBlocking { authLocal.loadSession()?.accessToken }
            // 다른 요청이 이미 갱신했으면 그 토큰으로 재시도, 아니면 직접 갱신
            val token = if (!stored.isNullOrEmpty() && stored != failedToken) {
                stored
            } else {
                runBlocking { refresher.refresh() }
            }
            return token?.let {
                response.request.newBuilder()
                    .header("Authorization", "Bearer $it")
                    .build()
            }
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    private companion object {
        const val MAX_RETRY = 2
    }
}
