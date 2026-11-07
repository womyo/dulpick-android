package com.dulpick.app.feature.course

import com.dulpick.app.domain.place.Coordinate
import com.dulpick.app.domain.place.PlaceCategory
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// 코스 수정 화면에 이미 담긴 장소. 장소 추가 화면이 지도에 기존 코스를 그릴 때 쓴다.
//
// 두 화면은 서로의 타입을 모른다. 내비게이션이 이 값을 그대로 건넨다 (iOS ExistingCoursePlace 대응)
data class ExistingCoursePlace(
    val id: String,
    val category: PlaceCategory,
    val coordinate: Coordinate,
)

// 위 값을 라우트 인자로 실어 보낼 때 쓰는 껍데기.
// domain 의 Coordinate 에 직렬화 어노테이션을 붙이지 않으려고 좌표를 둘로 풀어 담는다
// (PickedPlacesArg 와 같은 방식이다)
@Serializable
data class ExistingPlacesArg(val places: List<ExistingPlaceArg>) {
    fun encode(): String = Json.encodeToString(this)

    fun toExisting(): List<ExistingCoursePlace> = places.map { it.toExisting() }

    companion object {
        fun from(places: List<ExistingCoursePlace>): ExistingPlacesArg = ExistingPlacesArg(
            places.map {
                ExistingPlaceArg(
                    id = it.id,
                    category = it.category,
                    latitude = it.coordinate.latitude,
                    longitude = it.coordinate.longitude,
                )
            },
        )

        fun decode(raw: String): List<ExistingCoursePlace> =
            runCatching { Json.decodeFromString<ExistingPlacesArg>(raw) }.getOrNull()
                ?.toExisting()
                .orEmpty()
    }
}

@Serializable
data class ExistingPlaceArg(
    val id: String,
    val category: PlaceCategory,
    val latitude: Double,
    val longitude: Double,
) {
    fun toExisting(): ExistingCoursePlace =
        ExistingCoursePlace(id, category, Coordinate(latitude, longitude))
}
