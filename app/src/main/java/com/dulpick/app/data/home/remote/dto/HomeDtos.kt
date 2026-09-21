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

// 지난 데이트 코스 목록(GET /date-courses/past). totalCount 는 전체 데이트 횟수
@Serializable
data class PastDateCoursesResponseDto(
    val dateCourses: List<HomeDateCourseDto> = emptyList(),
    val totalCount: Int = 0,
    val hasNext: Boolean = false,
)

// 최근 저장 장소 항목 (iOS SavedPlaceResponseDTO 대응).
// 좌표·주소도 받는다. 빠뜨리면 홈에서 지도 상세로 넘긴 장소가 (0,0) 좌표가 돼 지도가 엉뚱한 곳을 본다
@Serializable
data class SavedPlaceItemDto(
    val placeId: Long,
    val kakaoPlaceId: String? = null,
    val name: String = "",
    val address: String = "",
    val roadAddress: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val category: String? = null,
    val categoryName: String = "",
    val thumbnailUrl: String? = null,
    val imageUrls: List<String> = emptyList(),
)
