package com.dulpick.app.domain.location

import com.dulpick.app.domain.place.Coordinate

// Feature 는 이 인터페이스로만 위치에 접근한다 (iOS LocationClient 대응)
interface LocationRepository {
    // 지금 권한 상태를 읽는다. 시스템 창을 띄우지 않는다
    fun authorization(): LocationAuthorization

    // 좌표를 한 번 읽는다. 따라다니지 않는다
    suspend fun currentCoordinate(): Coordinate
}
