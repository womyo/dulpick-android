package com.dulpick.app.core.map

import android.graphics.Bitmap
import com.dulpick.app.domain.place.Coordinate

// 핀 하나를 어떻게 그릴지. 같은 styleId 를 쓰는 핀은 이 값을 공유한다 — SDK 에 한 번만 등록한다
// (iOS MapPinStyle 대응)
data class MapPinStyle(
    val bitmap: Bitmap,
    // 그림의 어느 점이 좌표에 놓이는지. (0.5, 0.5) 면 그림 한가운데다
    val anchorX: Float,
    val anchorY: Float,
)

// 지도에 찍는 핀 하나. 이 계층은 핀이 무엇을 뜻하는지 모른다 — 장소인지 코스 번호인지는 화면이 안다
// (iOS MapPin 대응)
class MapPin(
    // 핀을 탭했을 때 화면이 무엇인지 찾는 열쇠. null 이면 탭해도 무시된다
    val id: String?,
    val coordinate: Coordinate,
    // 같은 값이면 같은 그림·기준점을 쓴다는 약속이다. 이 계층은 문자열의 뜻을 모른다
    val styleId: String,
    // 겹쳐 그릴 때의 순서. 큰 쪽이 위다
    val rank: Long = 0L,
    // styleId 를 처음 볼 때 한 번만 부른다. 그림 만들기가 비싸 핀마다 미리 만들지 않는다
    val makeStyle: () -> MapPinStyle,
) {
    // makeStyle 은 비교에서 뺀다. 함수는 비교할 수 없고,
    // 같은 styleId 면 같은 그림이라는 약속이 그 자리를 대신한다
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MapPin) return false
        return id == other.id &&
            coordinate == other.coordinate &&
            styleId == other.styleId &&
            rank == other.rank
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + coordinate.hashCode()
        result = 31 * result + styleId.hashCode()
        result = 31 * result + rank.hashCode()
        return result
    }
}

// 지도가 보여줄 자리 (iOS MapCamera 대응)
data class MapCamera(val center: Coordinate, val zoomLevel: Int) {
    companion object {
        // 여러 장소가 한눈에 보이는 줌
        const val MULTI_PLACE_ZOOM = 14

        // 장소 하나를 볼 때의 줌
        const val SINGLE_PLACE_ZOOM = 16

        // 보여줄 장소가 없을 때 서는 자리. 서울 시청이다
        val SEOUL_CITY_HALL = MapCamera(Coordinate(37.5666, 126.9784), MULTI_PLACE_ZOOM)
    }
}
