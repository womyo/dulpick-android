package com.dulpick.app.data.place.mapper

import com.dulpick.app.data.place.remote.dto.PlaceSearchItemDto
import com.dulpick.app.data.place.remote.dto.PlaceSearchResponseDto
import com.dulpick.app.data.place.remote.dto.SavedPlaceResponseDto
import com.dulpick.app.domain.place.Coordinate
import com.dulpick.app.domain.place.Place
import com.dulpick.app.domain.place.PlaceOwnership
import com.dulpick.app.domain.place.PlacePage
import com.dulpick.app.domain.place.SavedPlace

object PlaceDtoMapper {
    fun toDomain(dto: PlaceSearchResponseDto): PlacePage =
        PlacePage(items = dto.places.map(::toPlace), hasNext = dto.hasNext)

    fun toSavedPlaces(dtos: List<SavedPlaceResponseDto>): List<SavedPlace> =
        dtos.map(::toSavedPlace)

    private fun toPlace(dto: PlaceSearchItemDto): Place =
        Place(
            // placeId 없으면 kakaoPlaceId, 둘 다 없으면 이름·좌표로 결정적 식별자를 만든다
            id = dto.placeId?.toString() ?: dto.kakaoPlaceId ?: "${dto.name}|${dto.latitude}|${dto.longitude}",
            name = dto.name,
            category = PlaceCategoryMapper.fromCodeOrName(dto.categoryCode, dto.categoryName),
            bookmarkCount = 0,
            thumbnailUrls = dto.imageUrls,
            kakaoPlaceId = dto.kakaoPlaceId,
            coordinate = Coordinate(dto.latitude, dto.longitude),
        )

    fun toSavedPlace(dto: SavedPlaceResponseDto): SavedPlace =
        SavedPlace(
            place = Place(
                id = dto.placeId.toString(),
                name = dto.name,
                category = PlaceCategoryMapper.fromCodeOrName(dto.category, dto.categoryName),
                bookmarkCount = 0,
                thumbnailUrls = dto.imageUrls,
                kakaoPlaceId = dto.kakaoPlaceId,
                address = dto.address,
                roadAddress = dto.roadAddress.orEmpty(),
                coordinate = Coordinate(dto.latitude, dto.longitude),
            ),
            ownership = ownership(dto.ownershipStatus),
            alias = dto.alias,
            savedAt = dto.savedAt,
        )

    // 아는 값만 그대로 옮긴다. 모르는 값을 together 로 두면 그 장소가 '내가 저장'·'상대가 저장'
    // 필터에도 다 걸려 잘못 보인다. iOS 처럼 mine 으로 떨어뜨린다
    private fun ownership(raw: String): PlaceOwnership =
        when (raw.lowercase()) {
            "partner" -> PlaceOwnership.PARTNER
            "together" -> PlaceOwnership.TOGETHER
            else -> PlaceOwnership.MINE
        }
}
