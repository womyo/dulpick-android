package com.dulpick.app.data.place.remote.dto

import kotlinx.serialization.Serializable

// 장소 검색 결과. 그리드가 쓰는 필드만 선언. placeId 는 미저장 장소라 null 로 온다
@Serializable
data class PlaceSearchResponseDto(
    val places: List<PlaceSearchItemDto> = emptyList(),
    val hasNext: Boolean = false,
)

@Serializable
data class PlaceSearchItemDto(
    val placeId: Long? = null,
    val kakaoPlaceId: String? = null,
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    // 검색 응답엔 ASCII 코드가 실려 온다. 한글 이름보다 이쪽을 먼저 본다
    val categoryCode: String? = null,
    val categoryName: String = "",
    val imageUrls: List<String> = emptyList(),
)

// 별칭 수정 요청 (PATCH /api/v1/places/{id}/alias)
@Serializable
data class PlaceAliasRequestDto(
    val alias: String?,
)

// 저장한 장소 (GET /api/v1/places) (iOS SavedPlaceResponseDTO 대응)
@Serializable
data class SavedPlaceResponseDto(
    val placeId: Long = 0,
    val kakaoPlaceId: String? = null,
    val name: String = "",
    val address: String = "",
    // 도로명이 없는 장소가 있어 nullable
    val roadAddress: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val category: String = "",
    val categoryName: String = "",
    val ownershipStatus: String = "",
    val alias: String? = null,
    val savedAt: String? = null,
    val thumbnailUrl: String? = null,
    val imageUrls: List<String> = emptyList(),
)
