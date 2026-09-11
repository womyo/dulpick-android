package com.dulpick.app.data.auth.remote

import com.dulpick.app.data.auth.remote.dto.AuthNonceDto
import com.dulpick.app.data.auth.remote.dto.AuthTokenDto
import com.dulpick.app.data.auth.remote.dto.LogoutRequest
import com.dulpick.app.data.auth.remote.dto.NonceRequest
import com.dulpick.app.data.auth.remote.dto.ReissueRequest
import com.dulpick.app.data.auth.remote.dto.SocialLoginRequest
import com.dulpick.app.data.auth.remote.dto.SocialLoginResponseDto
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("/api/v1/auth/nonce")
    suspend fun issueNonce(@Body body: NonceRequest): AuthNonceDto

    @POST("/api/v1/auth/social-login")
    suspend fun socialLogin(@Body body: SocialLoginRequest): SocialLoginResponseDto

    @POST("/api/v1/auth/reissue")
    suspend fun reissue(@Body body: ReissueRequest): AuthTokenDto

    @POST("/api/v1/auth/logout")
    suspend fun logout(@Body body: LogoutRequest)
}
