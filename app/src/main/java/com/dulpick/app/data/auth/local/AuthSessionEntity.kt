package com.dulpick.app.data.auth.local

import kotlinx.serialization.Serializable

// 값이 없던 기존 저장본도 디코딩되도록 플래그는 nullable
@Serializable
data class AuthSessionEntity(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val isOnboardingCompleted: Boolean? = null,
)
