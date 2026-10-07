package com.dulpick.app.domain.notice

// Data 에서 Core/네트워크 에러를 이걸로 매핑한다 (iOS NoticeError 대응).
// 공지는 로그인 없이 읽어 권한 오류를 따로 두지 않는다
sealed class NoticeError : Exception() {
    // 네트워크 단절/전송 실패
    data object Network : NoticeError()

    // 분류되지 않은 실패
    data object Unknown : NoticeError()
}
