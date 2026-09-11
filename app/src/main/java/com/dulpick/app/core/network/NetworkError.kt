package com.dulpick.app.core.network

// Data 에서 도메인 에러로 매핑된다
sealed class NetworkError : Exception() {
    // 401 계열
    data object Unauthorized : NetworkError()

    // 네트워크 단절/전송 실패
    data object Network : NetworkError()

    // 그 외 HTTP 에러
    data class Server(val code: Int) : NetworkError()

    // 응답 디코딩 실패
    data object Serialization : NetworkError()

    // 분류되지 않은 실패
    data object Unknown : NetworkError()
}
