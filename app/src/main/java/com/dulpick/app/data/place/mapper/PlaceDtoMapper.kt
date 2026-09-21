package com.dulpick.app.data.place.mapper

import com.dulpick.app.data.place.remote.dto.PlaceDetailResponseDto
import com.dulpick.app.data.place.remote.dto.PlaceSearchItemDto
import com.dulpick.app.data.place.remote.dto.PlaceSearchResponseDto
import com.dulpick.app.data.place.remote.dto.SavedPlaceResponseDto
import com.dulpick.app.domain.place.Coordinate
import com.dulpick.app.domain.place.Place
import com.dulpick.app.domain.place.PlaceDetail
import com.dulpick.app.domain.place.PlaceOwnership
import com.dulpick.app.domain.place.PlacePage
import com.dulpick.app.domain.place.SavedPlace

object PlaceDtoMapper {
    fun toDomain(dto: PlaceSearchResponseDto): PlacePage =
        PlacePage(items = dto.places.map(::toPlace), hasNext = dto.hasNext)

    fun toSavedPlaces(dtos: List<SavedPlaceResponseDto>): List<SavedPlace> =
        dtos.map(::toSavedPlace)

    fun toDetail(dto: PlaceDetailResponseDto): PlaceDetail = PlaceDetail(
        place = Place(
            // 상세 응답은 kakaoPlaceId 가 항상 있어 합성 폴백이 필요 없다
            id = dto.placeId?.toString() ?: dto.kakaoPlaceId,
            name = dto.name,
            category = PlaceCategoryMapper.fromCodeOrName(dto.categoryCode, dto.categoryName),
            // 상세 응답엔 북마크 수가 없다. 화면은 savedMemberCount 를 쓴다
            bookmarkCount = 0,
            thumbnailUrls = dto.imageUrls,
            kakaoPlaceId = dto.kakaoPlaceId,
            address = dto.address,
            roadAddress = dto.roadAddress.orEmpty(),
            coordinate = Coordinate(dto.latitude ?: 0.0, dto.longitude ?: 0.0),
        ),
        // 서버가 준 placeId 그대로. 미저장 장소는 null → 게시물·삭제에서 서버 ID 로 쓰지 않는다
        serverPlaceId = dto.placeId,
        savedByMe = dto.savedByMe,
        savedMemberCount = dto.savedMemberCount,
        ownership = dto.ownershipStatus?.let(::ownership),
        phone = dto.phone,
        kakaoPlaceUrl = dto.kakaoPlaceUrl,
    )

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

    private fun ownership(raw: String): PlaceOwnership =
        when (raw.lowercase()) {
            "mine" -> PlaceOwnership.MINE
            "partner" -> PlaceOwnership.PARTNER
            else -> PlaceOwnership.TOGETHER
        }
}
