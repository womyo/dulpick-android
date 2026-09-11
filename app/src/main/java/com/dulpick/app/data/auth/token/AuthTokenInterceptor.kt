package com.dulpick.app.data.auth.token

import com.dulpick.app.data.auth.local.AuthLocalDataSource
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

// 저장된 access token 을 Bearer 로 붙인다
class AuthTokenInterceptor(
    private val authLocal: AuthLocalDataSource,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { authLocal.loadSession()?.accessToken }
        val request = if (token.isNullOrEmpty()) {
            chain.request()
        } else {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        }
        return chain.proceed(request)
    }
}
