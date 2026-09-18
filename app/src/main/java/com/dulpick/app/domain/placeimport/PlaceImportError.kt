package com.dulpick.app.domain.placeimport

// Data 에서 Core/네트워크 에러를 이걸로 매핑한다 (iOS PlaceImportError 대응)
sealed class PlaceImportError : Exception() {
    // 네트워크 단절/전송 실패
    data object Network : PlaceImportError()

    // 세션 만료, 인증 실패(401)
    data object Unauthorized : PlaceImportError()

    // 추출 세션 없음(404). 폴링 대상이 사라진 경우
    data object NotFound : PlaceImportError()

    // 권한 없음(403)
    data object Forbidden : PlaceImportError()

    // 요청 과다(429)
    data object RateLimited : PlaceImportError()

    // 분류되지 않은 실패
    data object Unknown : PlaceImportError()
}
