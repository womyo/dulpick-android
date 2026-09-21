package com.dulpick.app.feature.placedetail

import com.dulpick.app.domain.place.Coordinate
import com.dulpick.app.domain.place.Place
import com.dulpick.app.domain.place.PlaceCategory
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// 상세로 진입할 때 Place 를 nav 인자로 통째로 넘긴다. iOS 처럼 상세 API 가 실패해도 이 값으로 화면을 그린다.
// domain Place 에 직렬화 어노테이션을 붙이지 않으려고 화면 레이어의 별도 타입으로 감싼다
@kotlinx.serialization.Serializable
data class PlaceArg(
    val id: String,
    val name: String,
    val category: PlaceCategory,
    val bookmarkCount: Int,
    val thumbnailUrls: List<String>,
    val kakaoPlaceId: String?,
    val address: String,
    val roadAddress: String,
    val latitude: Double,
    val longitude: Double,
) {
    fun toPlace(): Place = Place(
        id = id,
        name = name,
        category = category,
        bookmarkCount = bookmarkCount,
        thumbnailUrls = thumbnailUrls,
        kakaoPlaceId = kakaoPlaceId,
        address = address,
        roadAddress = roadAddress,
        coordinate = Coordinate(latitude, longitude),
    )

    companion object {
        fun from(place: Place): PlaceArg = PlaceArg(
            id = place.id,
            name = place.name,
            category = place.category,
            bookmarkCount = place.bookmarkCount,
            thumbnailUrls = place.thumbnailUrls,
            kakaoPlaceId = place.kakaoPlaceId,
            address = place.address,
            roadAddress = place.roadAddress,
            latitude = place.coordinate.latitude,
            longitude = place.coordinate.longitude,
        )
    }
}

// 지도 검색 결과 → 지도로 상세 대상을 되돌려줄 때 쓰는 인자(장소 + 검색어)
@kotlinx.serialization.Serializable
data class MapDetailArg(
    val place: PlaceArg,
    val query: String,
) {
    fun encode(): String = Json.encodeToString(this)

    companion object {
        fun of(place: Place, query: String): MapDetailArg = MapDetailArg(PlaceArg.from(place), query)

        fun decode(raw: String): MapDetailArg? =
            runCatching { Json.decodeFromString<MapDetailArg>(raw) }.getOrNull()
    }
}
