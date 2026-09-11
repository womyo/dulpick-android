package com.dulpick.app.domain.auth

data class AuthSession(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
)
