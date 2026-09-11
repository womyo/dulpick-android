package com.dulpick.app.domain.auth

data class AuthBootstrap(
    val session: AuthSession,
    val isOnboardingCompleted: Boolean,
    // 가입일 기록·첫 로그인 이벤트를 가른다. 로그인 응답에서만 참, 세션 복구에선 항상 거짓
    val isNewMember: Boolean,
)
