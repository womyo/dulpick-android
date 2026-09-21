package com.dulpick.app.domain.place

// Data 에서 Core/네트워크 에러를 이걸로 매핑한다 (iOS PlaceError 대응)
sealed class PlaceError : Exception() {
    // 네트워크 단절/전송 실패
    data object Network : PlaceError()

    // 세션 만료, 인증 실패(401)
    data object Unauthorized : PlaceError()

    // 대상 장소를 서버가 찾지 못함(404). 상대가 저장한 장소 등 내 대상이 아닌 경우
    data object NotFound : PlaceError()

    // 이미 내 저장 목록에 있는 장소를 다시 저장하려 함(409)
    data object AlreadySaved : PlaceError()

    // 분류되지 않은 실패
    data object Unknown : PlaceError()
}
