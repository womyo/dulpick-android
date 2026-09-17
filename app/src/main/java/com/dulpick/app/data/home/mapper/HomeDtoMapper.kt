package com.dulpick.app.data.home.mapper

import com.dulpick.app.data.home.remote.dto.HomeDateCourseDto
import com.dulpick.app.data.home.remote.dto.HomeSummaryResponseDto
import com.dulpick.app.data.home.remote.dto.SavedPlaceItemDto
import com.dulpick.app.data.place.mapper.PlaceCategoryMapper
import com.dulpick.app.domain.course.DateCourseSummary
import com.dulpick.app.domain.home.DateSchedule
import com.dulpick.app.domain.home.HomeSummary
import com.dulpick.app.domain.place.Place

object HomeDtoMapper {
    fun toSummary(dto: HomeSummaryResponseDto): HomeSummary =
        HomeSummary(
            connected = dto.connected,
            myNickname = dto.myNickname ?: "",
            partnerNickname = if (dto.connected) dto.partnerNickname else null,
            // 미연결이면 현재 코스는 없다
            currentDateCourse = if (dto.connected) dto.currentDateCourse?.let(::toCourseSummary) else null,
        )

    fun toPastDates(dtos: List<HomeDateCourseDto>): List<DateSchedule> = dtos.map(::toDateSchedule)

    fun toSavedPlaces(dtos: List<SavedPlaceItemDto>): List<Place> = dtos.map(::toPlace)

    private fun toCourseSummary(dto: HomeDateCourseDto): DateCourseSummary =
        DateCourseSummary(
            id = dto.dateCourseId.toString(),
            title = dto.title,
            totalPlaceCount = dto.totalPlaceCount,
        )

    private fun toDateSchedule(dto: HomeDateCourseDto): DateSchedule =
        DateSchedule(
            id = dto.dateCourseId.toString(),
            title = dto.title,
            placeCount = dto.totalPlaceCount,
            // 지난 데이트는 yy.MM.dd
            date = shortDate(dto.date),
        )

    private fun toPlace(dto: SavedPlaceItemDto): Place =
        Place(
            id = dto.placeId.toString(),
            name = dto.name,
            // 저장 목록엔 code 가 없어 한글 categoryName 으로 매핑한다
            category = PlaceCategoryMapper.fromName(dto.categoryName),
            bookmarkCount = 0,
            thumbnailUrls = dto.imageUrls,
        )

    // "2026-08-16" → "26.08.16"
    private fun shortDate(raw: String): String {
        val parts = raw.split("-")
        if (parts.size != 3) return raw
        return "${parts[0].takeLast(2)}.${parts[1]}.${parts[2]}"
    }
}
