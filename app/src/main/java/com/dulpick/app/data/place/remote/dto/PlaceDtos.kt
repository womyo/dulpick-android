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
    val address: String = "",
    val roadAddress: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    // 검색 응답엔 ASCII 코드가 실려 온다. 한글 이름보다 이쪽을 먼저 본다
    val categoryCode: String? = null,
    val categoryName: String = "",
    val thumbnailUrl: String? = null,
    val imageUrls: List<String> = emptyList(),
)

// 장소 저장 요청 (POST /api/v1/places) (iOS PlaceSaveRequestDTO 대응)
@Serializable
data class PlaceSaveRequestDto(
    val kakaoPlaceId: String,
    val query: String,
    val alias: String? = null,
)

// 별칭 수정 요청 (PATCH /api/v1/places/{id}/alias)
@Serializable
data class PlaceAliasRequestDto(
    val alias: String?,
)

// 장소 상세 (GET /places/{id}, GET /places/kakao/{kakaoId}) (iOS PlaceDetailResponseDTO 대응).
// 좌표·category 는 명세상 nullable 이라 방어적으로 옵셔널·기본값을 둔다
@Serializable
data class PlaceDetailResponseDto(
    val placeId: Long? = null,
    val kakaoPlaceId: String = "",
    val name: String = "",
    val address: String = "",
    val roadAddress: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val category: String? = null,
    val categoryCode: String? = null,
    val categoryName: String = "",
    val phone: String? = null,
    val kakaoPlaceUrl: String? = null,
    val savedByMe: Boolean = false,
    val ownershipStatus: String? = null,
    val thumbnailUrl: String? = null,
    val imageUrls: List<String> = emptyList(),
    val savedMemberCount: Int = 0,
)

// 저장한 장소 (GET /api/v1/places) (iOS SavedPlaceResponseDTO 대응)
@Serializable
data class SavedPlaceResponseDto(
    // 서버 계약상 필수인 필드는 기본값을 두지 않는다. 기본값을 두면 누락을
    // placeId "0", 좌표 (0,0), 소유권 같은 그럴듯한 값으로 바꿔 조용히 잘못 표시한다
    val placeId: Long,
    val kakaoPlaceId: String? = null,
    val name: String,
    val address: String,
    // 도로명이 없는 장소가 있어 nullable
    val roadAddress: String? = null,
    val latitude: Double,
    val longitude: Double,
    val category: String,
    val categoryName: String,
    val ownershipStatus: String,
    val alias: String? = null,
    val savedAt: String? = null,
    val thumbnailUrl: String? = null,
    val imageUrls: List<String> = emptyList(),
)
