package com.dulpick.app.domain.location

// 위치 권한 상태. 시스템 값을 화면이 갈라야 하는 세 갈래로 줄인 것이다 (iOS LocationAuthorization 대응)
enum class LocationAuthorization {
    // 아직 물어본 적이 없다. 버튼을 누르면 시스템 권한 요청을 띄운다
    NOT_DETERMINED,

    // 정밀 또는 대략 위치 허용
    AUTHORIZED,

    // 다시 묻지 않음으로 거부됐다. 설정으로 보내는 수밖에 없다
    DENIED,
}

// 좌표 1회 조회가 실패하는 까닭.
// 화면은 둘을 안 가리고 같은 토스트를 띄운다. 나눠 둔 것은 로그를 읽을 때를 위해서다
sealed class LocationError : Exception() {
    // 권한이 없어 좌표를 못 얻는다
    data object Denied : LocationError()

    // 기기가 좌표를 못 준다. 시간 초과를 포함한다
    data object Unavailable : LocationError()
}
