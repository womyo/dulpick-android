package com.dulpick.app.data.auth.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class NonceRequest(val provider: String)

@Serializable
data class SocialLoginRequest(
    val provider: String,
    val idToken: String,
    val authorizationCode: String? = null,
    val nonce: String,
)

@Serializable
data class ReissueRequest(val refreshToken: String)

@Serializable
data class LogoutRequest(val refreshToken: String)
