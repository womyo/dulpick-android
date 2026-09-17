package com.dulpick.app.domain.profile

sealed class ProfileError : Exception() {
    // 닉네임 형식/정책 위반
    data object InvalidNickname : ProfileError()

    // 네트워크 단절/전송 실패
    data object Network : ProfileError()

    // 세션 만료, 인증 실패(401 계열)
    data object Unauthorized : ProfileError()

    // 분류되지 않은 실패
    data object Unknown : ProfileError()
}
