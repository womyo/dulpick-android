package com.dulpick.app.data.place.mapper

import com.dulpick.app.data.place.remote.dto.PlaceSearchItemDto
import com.dulpick.app.data.place.remote.dto.PlaceSearchResponseDto
import com.dulpick.app.domain.place.Place
import com.dulpick.app.domain.place.PlacePage

object PlaceDtoMapper {
    fun toDomain(dto: PlaceSearchResponseDto): PlacePage =
        PlacePage(items = dto.places.map(::toPlace), hasNext = dto.hasNext)

    private fun toPlace(dto: PlaceSearchItemDto): Place =
        Place(
            // placeId 없으면 kakaoPlaceId, 둘 다 없으면 이름·좌표로 결정적 식별자를 만든다
            id = dto.placeId?.toString() ?: dto.kakaoPlaceId ?: "${dto.name}|${dto.latitude}|${dto.longitude}",
            name = dto.name,
            category = PlaceCategoryMapper.fromCodeOrName(dto.categoryCode, dto.categoryName),
            bookmarkCount = 0,
            thumbnailUrls = dto.imageUrls,
        )
}
