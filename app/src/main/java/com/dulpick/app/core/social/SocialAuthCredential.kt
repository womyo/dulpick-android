package com.dulpick.app.core.social

// 소셜 SDK 로그인 결과. 백엔드 social-login 검증에 넘긴다
data class SocialAuthCredential(
    val idToken: String,
    val authorizationCode: String? = null,
)
