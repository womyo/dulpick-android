package com.dulpick.app.data.auth.token

import com.dulpick.app.data.auth.local.AuthLocalDataSource
import com.dulpick.app.data.auth.mapper.AuthMapper
import com.dulpick.app.data.auth.remote.AuthApi
import com.dulpick.app.data.auth.remote.dto.ReissueRequest

// reissue 로 토큰을 회전한다
// 순환 의존을 피하려 authed 클라이언트가 아니라 plain AuthApi 를 직접 쓴다
class AuthTokenRefresher(
    private val authLocal: AuthLocalDataSource,
    private val plainApi: AuthApi,
) {
    // 실패하면 null. authenticator 가 재시도를 포기하고 401 을 그대로 흘린다
    @Suppress("TooGenericExceptionCaught")
    suspend fun refresh(): String? {
        val current = authLocal.loadSession() ?: return null
        return try {
            val token = plainApi.reissue(ReissueRequest(refreshToken = current.refreshToken))
            val rotated = AuthMapper.toEntity(token, current = current)
            authLocal.saveSession(rotated)
            rotated.accessToken
        } catch (error: Exception) {
            null
        }
    }
}
