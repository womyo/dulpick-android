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
