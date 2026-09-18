package com.dulpick.app.data.place.mapper

import com.dulpick.app.data.place.remote.dto.PlaceSearchItemDto
import com.dulpick.app.data.place.remote.dto.PlaceSearchResponseDto
import com.dulpick.app.domain.place.Place
import com.dulpick.app.domain.place.PlaceCategory
import com.dulpick.app.domain.place.PlacePage

object PlaceDtoMapper {
    fun toDomain(dto: PlaceSearchResponseDto): PlacePage =
        PlacePage(items = dto.places.map(::toPlace), hasNext = dto.hasNext)

    private fun toPlace(dto: PlaceSearchItemDto): Place =
        Place(
            // placeId 없으면 kakaoPlaceId, 둘 다 없으면 이름·좌표로 결정적 식별자를 만든다
            id = dto.placeId?.toString() ?: dto.kakaoPlaceId ?: "${dto.name}|${dto.latitude}|${dto.longitude}",
            name = dto.name,
            category = category(dto.categoryCode, dto.categoryName),
            bookmarkCount = 0,
            thumbnailUrls = dto.imageUrls,
        )

    // 검색 응답은 ASCII categoryCode 우선, 없으면 한글 categoryName 으로 매핑 (iOS 동일)
    private fun category(code: String?, name: String): PlaceCategory =
        when (code) {
            "RESTAURANT" -> PlaceCategory.FOOD
            "CAFE" -> PlaceCategory.CAFE
            "ENTERTAINMENT" -> PlaceCategory.ACTIVITY
            "SHOPPING" -> PlaceCategory.SHOPPING
            "CONVENIENCE" -> PlaceCategory.CONVENIENCE
            "TOURISM" -> PlaceCategory.TOURISM
            "ACCOMMODATION" -> PlaceCategory.ACCOMMODATION
            else -> categoryByName(name)
        }

    private fun categoryByName(name: String): PlaceCategory =
        when (name) {
            "카페" -> PlaceCategory.CAFE
            "관광" -> PlaceCategory.TOURISM
            "놀거리" -> PlaceCategory.ACTIVITY
            "쇼핑" -> PlaceCategory.SHOPPING
            "숙박" -> PlaceCategory.ACCOMMODATION
            "편의", "생활 편의" -> PlaceCategory.CONVENIENCE
            else -> PlaceCategory.FOOD
        }
}
