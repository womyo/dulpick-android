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
