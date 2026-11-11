package com.dulpick.app.domain.location

import com.dulpick.app.domain.place.Coordinate
import kotlinx.coroutines.flow.Flow

// Feature 는 이 인터페이스로만 위치에 접근한다 (iOS LocationClient 대응)
interface LocationRepository {
    // 지금 권한 상태를 읽는다. 시스템 창을 띄우지 않는다
    fun authorization(): LocationAuthorization

    // 좌표를 한 번 읽는다. 따라다니지 않는다
    suspend fun currentCoordinate(): Coordinate

    // 내 위치를 계속 흘린다. 권한을 요청하지 않는다.
    // 쓸 수 있는 좌표면 그 값을, 정밀 권한이 없으면 null 을 보낸다. 구독을 끊으면 갱신이 멈춘다
    fun coordinateUpdates(): Flow<Coordinate?>
}
