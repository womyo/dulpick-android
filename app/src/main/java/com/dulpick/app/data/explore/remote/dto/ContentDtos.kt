package com.dulpick.app.data.explore.remote.dto

import kotlinx.serialization.Serializable

// 그리드가 쓰는 필드만 선언. author/engagement/places 등 나머지 키는 무시된다
@Serializable
data class ContentPageResponseDto(
    val contents: List<ContentResponseDto> = emptyList(),
    val hasNext: Boolean = false,
    val popularTags: List<String>? = null,
)

@Serializable
data class ContentResponseDto(
    val contentId: Long,
    val title: String = "",
    val thumbnailUrl: String? = null,
    val placeCount: Int = 0,
)

// 게시글 상세. 화면이 쓰는 필드만 선언한다
@Serializable
data class ContentDetailResponseDto(
    val contentId: Long,
    val title: String? = null,
    val caption: String? = null,
    val canonicalUrl: String? = null,
    val places: List<ContentDetailPlaceResponseDto>? = null,
)

@Serializable
data class ContentDetailPlaceResponseDto(
    val placeId: Long,
    val kakaoPlaceId: String? = null,
    val name: String,
    // 지번 주소는 명세상 필수다. 도로명은 없는 장소가 있어 옵셔널
    val address: String,
    val roadAddress: String? = null,
    val categoryName: String,
    val latitude: Double,
    val longitude: Double,
    val savedByMe: Boolean,
    val thumbnailUrl: String? = null,
    // 이미지가 없는 장소도 있어 옵셔널
    val imageUrls: List<String>? = null,
)
