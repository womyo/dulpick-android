package com.dulpick.app.data.auth.token

import com.dulpick.app.data.auth.local.AuthLocalDataSource
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
            // 네트워크 대기 중 로그아웃·온보딩 갱신이 끼어들 수 있어, 저장은 원자적 CAS 로 맡긴다.
            // 세션이 사라졌거나 이미 회전됐으면 null 이 돌아와 authenticator 가 재시도를 포기한다
            authLocal.rotateTokens(
                expectedRefreshToken = current.refreshToken,
                newAccessToken = token.accessToken,
                newRefreshToken = token.refreshToken,
            )?.accessToken
        } catch (error: Exception) {
            null
        }
    }
}
