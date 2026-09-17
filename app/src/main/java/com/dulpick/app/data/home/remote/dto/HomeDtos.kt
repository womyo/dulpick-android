package com.dulpick.app.data.home.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class HomeSummaryResponseDto(
    val connected: Boolean = false,
    val myNickname: String? = null,
    val partnerNickname: String? = null,
    val currentDateCourse: HomeDateCourseDto? = null,
)

// 지난 데이트 목록엔 status·version 이 안 와 화면에 필요한 필드만 선언한다
@Serializable
data class HomeDateCourseDto(
    val dateCourseId: Long,
    val title: String = "",
    val date: String = "",
    val totalPlaceCount: Int = 0,
)

// 최근 저장 장소 항목. 홈 행이 쓰는 이름·카테고리·썸네일만 선언한다
@Serializable
data class SavedPlaceItemDto(
    val placeId: Long,
    val kakaoPlaceId: String? = null,
    val name: String = "",
    val categoryName: String = "",
    val thumbnailUrl: String? = null,
    val imageUrls: List<String> = emptyList(),
)
