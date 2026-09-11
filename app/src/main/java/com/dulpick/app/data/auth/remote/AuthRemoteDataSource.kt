package com.dulpick.app.data.auth.remote

import com.dulpick.app.core.network.Authed
import com.dulpick.app.core.network.Plain
import com.dulpick.app.core.network.safeApiCall
import com.dulpick.app.data.auth.remote.dto.AuthNonceDto
import com.dulpick.app.data.auth.remote.dto.AuthTokenDto
import com.dulpick.app.data.auth.remote.dto.LogoutRequest
import com.dulpick.app.data.auth.remote.dto.NonceRequest
import com.dulpick.app.data.auth.remote.dto.ReissueRequest
import com.dulpick.app.data.auth.remote.dto.SocialLoginRequest
import com.dulpick.app.data.auth.remote.dto.SocialLoginResponseDto
import com.dulpick.app.domain.auth.AuthProvider
import javax.inject.Inject

class AuthRemoteDataSource @Inject constructor(
    @Plain private val plainApi: AuthApi,
    @Authed private val authedApi: AuthApi,
) {
    suspend fun issueNonce(provider: AuthProvider): AuthNonceDto = safeApiCall {
        plainApi.issueNonce(NonceRequest(provider = provider.name))
    }

    suspend fun socialLogin(
        provider: AuthProvider,
        idToken: String,
        authorizationCode: String?,
        nonce: String,
    ): SocialLoginResponseDto = safeApiCall {
        plainApi.socialLogin(
            SocialLoginRequest(
                provider = provider.name,
                idToken = idToken,
                authorizationCode = authorizationCode,
                nonce = nonce,
            ),
        )
    }

    suspend fun reissue(refreshToken: String): AuthTokenDto = safeApiCall {
        plainApi.reissue(ReissueRequest(refreshToken = refreshToken))
    }

    suspend fun logout(refreshToken: String): Unit = safeApiCall {
        authedApi.logout(LogoutRequest(refreshToken = refreshToken))
    }
}
