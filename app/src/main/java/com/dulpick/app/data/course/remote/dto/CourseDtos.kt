package com.dulpick.app.data.course.remote.dto

import kotlinx.serialization.Serializable

// 코스 생성. 제목·날짜·시간만 보낸다. 장소는 안 보낸다
@Serializable
data class CreateDateCourseRequestDto(
    val title: String,
    // yyyy-MM-dd
    val date: String,
    // HH:mm:ss
    val time: String? = null,
)

// 확정 저장. version 은 낙관적 락 번호다
@Serializable
data class SaveDateCourseRequestDto(
    val title: String,
    val date: String,
    val time: String? = null,
    val placeIds: List<Long>,
    val version: Int,
)

@Serializable
data class DateCourseResponseDto(
    val dateCourseId: Long,
    val title: String,
    val date: String,
    val time: String? = null,
    val status: String,
    val version: Int,
    val totalPlaceCount: Int? = null,
    val places: List<DateCoursePlaceResponseDto>? = null,
)

@Serializable
data class DateCoursePlaceResponseDto(
    val order: Int,
    val placeId: Long,
    val name: String,
    val address: String,
    val roadAddress: String? = null,
    val latitude: Double,
    val longitude: Double,
    val category: String? = null,
    val categoryName: String? = null,
    val thumbnailUrl: String? = null,
    val imageUrls: List<String>? = null,
    val walkToNext: WalkToNextResponseDto? = null,
)

@Serializable
data class WalkToNextResponseDto(
    val distanceMeters: Int,
    val durationSeconds: Int,
)

@Serializable
data class CurrentDateCourseResponseDto(
    val currentDateCourse: DateCourseSummaryResponseDto? = null,
)

// 예정 데이트 요약. 화면이 읽는 필드만 선언한다(status·version 은 안 읽는다)
@Serializable
data class DateCourseSummaryResponseDto(
    val dateCourseId: Long,
    val title: String,
    val date: String,
    val time: String? = null,
    val totalPlaceCount: Int,
)

@Serializable
data class DateCoursePlacePoolResponseDto(
    val places: List<DateCoursePlaceCandidateResponseDto>,
)

// region·savedAt·category(카카오 분류 단계값) 는 담지 않는다. 장소 선택 화면이 안 읽는다
@Serializable
data class DateCoursePlaceCandidateResponseDto(
    val placeId: Long,
    val name: String,
    // 지번 주소는 명세상 필수다. 도로명은 없는 장소가 있어 옵셔널
    val address: String,
    val roadAddress: String? = null,
    val latitude: Double,
    val longitude: Double,
    val categoryName: String,
    val ownershipStatus: String,
    val alias: String? = null,
    val thumbnailUrl: String? = null,
    val imageUrls: List<String>,
)
