package com.dulpick.app.domain.explore

// Data 에서 Core/네트워크 에러를 이걸로 매핑한다
sealed class ExploreError : Exception() {
    // 네트워크 단절/전송 실패
    data object Network : ExploreError()

    // 세션 만료, 인증 실패(401 계열)
    data object Unauthorized : ExploreError()

    // 분류되지 않은 실패
    data object Unknown : ExploreError()
}
