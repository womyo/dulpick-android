package com.dulpick.app.data.auth.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class AuthNonceDto(
    val nonce: String,
    val expiresAt: String,
)

@Serializable
data class AuthTokenDto(
    val tokenType: String,
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Int,
)

@Serializable
data class SocialLoginResponseDto(
    val memberId: Int,
    val newMember: Boolean,
    // 서버가 생략해도 로그인이 깨지지 않도록 nullable
    val onboardingCompleted: Boolean? = null,
    val token: AuthTokenDto,
)
