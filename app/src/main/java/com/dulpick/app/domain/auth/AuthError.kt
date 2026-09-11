package com.dulpick.app.domain.auth

// throw 가능하도록 Exception 하위로 둔다
sealed class AuthError : Exception() {
    // 사용자가 소셜 로그인/인증 플로우를 취소
    data object Cancelled : AuthError()

    // nonce 발급, SDK 로그인, social-login 검증 등 로그인 흐름 실패
    data object LoginFailed : AuthError()

    // 네트워크 단절/전송 실패
    data object Network : AuthError()

    // 세션 만료, 인증 실패(401 계열)
    data object Unauthorized : AuthError()

    // 로컬 세션 저장/조회 실패
    data object Storage : AuthError()

    // 분류되지 않은 실패
    data object Unknown : AuthError()
}
