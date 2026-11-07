package com.dulpick.app.feature.course

import com.dulpick.app.domain.place.Coordinate
import com.dulpick.app.domain.place.PlaceCategory
import com.dulpick.app.domain.place.SavedPlace
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// 장소 추가 화면에서 고른 장소를 코스 수정 화면으로 되돌려줄 때 쓰는 인자.
// domain 모델에 직렬화 어노테이션을 붙이지 않으려고 화면 계층의 타입으로 감싼다
@Serializable
data class PickedPlacesArg(
    val places: List<PickedPlaceArg>,
    // 같은 결과가 두 번 반영되는 걸 막는 일회성 값
    val nonce: Long,
) {
    fun encode(): String = Json.encodeToString(this)

    companion object {
        fun from(places: List<SavedPlace>): PickedPlacesArg =
            PickedPlacesArg(
                places = places.map {
                    PickedPlaceArg(
                        id = it.id,
                        name = it.place.name,
                        category = it.place.category,
                        address = it.place.roadAddress.ifEmpty { it.place.address },
                        latitude = it.place.coordinate.latitude,
                        longitude = it.place.coordinate.longitude,
                    )
                },
                nonce = System.nanoTime(),
            )

        fun decode(raw: String): PickedPlacesArg? =
            runCatching { Json.decodeFromString<PickedPlacesArg>(raw) }.getOrNull()
    }
}

@Serializable
data class PickedPlaceArg(
    val id: String,
    val name: String,
    val category: PlaceCategory,
    val address: String,
    val latitude: Double,
    val longitude: Double,
) {
    fun toEditable(): EditablePlace = EditablePlace(
        id = id,
        name = name,
        category = category,
        address = address,
        coordinate = Coordinate(latitude, longitude),
    )
}
