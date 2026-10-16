package com.dulpick.app.domain.notification

// Data 에서 Core/네트워크 에러를 이걸로 매핑한다 (iOS NotificationError 대응)
sealed class PushError : Exception() {
    // 연결 실패·타임아웃
    data object Network : PushError()

    // 401. 세션이 없거나 만료됐다
    data object Unauthorized : PushError()

    // 400. 플랫폼·공급자·등록 토큰이 누락됐거나 허용되지 않은 값이다
    data object InvalidRequest : PushError()

    // 409. 푸시 기기 등록 상태가 충돌했다
    data object RegistrationConflict : PushError()

    // 503. 외부 연동 실패로 등록할 수 없다
    data object ProviderUnavailable : PushError()

    // 분류되지 않은 실패
    data object Unknown : PushError()
}
