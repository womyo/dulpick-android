package com.dulpick.app.domain.course

// Data 에서 Core/네트워크 에러를 이걸로 매핑한다 (iOS CourseError 대응)
sealed class CourseError : Exception() {
    // 네트워크 단절/전송 실패
    data object Network : CourseError()

    // 세션 만료, 인증 실패(401 계열)
    data object Unauthorized : CourseError()

    // 대상 코스를 서버가 찾지 못함
    data object NotFound : CourseError()

    // 상대가 먼저 고쳐서 version 이 어긋남. HTTP 409
    data object Conflict : CourseError()

    // 분류되지 않은 실패
    data object Unknown : CourseError()
}
