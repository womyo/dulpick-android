package com.dulpick.app.domain.couple

// Data 에서 Core/네트워크 에러를 이걸로 매핑한다 (iOS CoupleError 대응)
sealed class CoupleError : Exception() {
    // 초대 코드가 없거나 유효하지 않음
    data object InvalidInviteCode : CoupleError()

    // 이미 커플 연결 상태
    data object AlreadyConnected : CoupleError()

    // 요청 횟수 제한 초과
    data object RateLimited : CoupleError()

    // 네트워크 단절/전송 실패
    data object Network : CoupleError()

    // 세션 만료, 인증 실패(401 계열)
    data object Unauthorized : CoupleError()

    // 분류되지 않은 실패
    data object Unknown : CoupleError()
}
