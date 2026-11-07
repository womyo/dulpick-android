package com.dulpick.app.data.course.mapper

import com.dulpick.app.data.course.remote.dto.DateCoursePlaceCandidateResponseDto
import com.dulpick.app.data.course.remote.dto.DateCoursePlaceResponseDto
import com.dulpick.app.data.course.remote.dto.DateCoursePlaceSearchPageResponseDto
import com.dulpick.app.data.course.remote.dto.DateCourseResponseDto
import com.dulpick.app.data.course.remote.dto.DateCourseSummaryResponseDto
import com.dulpick.app.data.course.remote.dto.RoutePointResponseDto
import com.dulpick.app.data.course.remote.dto.TransitToNextResponseDto
import com.dulpick.app.data.course.remote.dto.WalkToNextResponseDto
import com.dulpick.app.data.place.mapper.PlaceCategoryMapper
import com.dulpick.app.domain.common.Page
import com.dulpick.app.domain.course.CourseError
import com.dulpick.app.domain.course.CourseLeg
import com.dulpick.app.domain.course.CourseStatus
import com.dulpick.app.domain.course.CourseStop
import com.dulpick.app.domain.course.CourseTransit
import com.dulpick.app.domain.course.DateCourse
import com.dulpick.app.domain.course.DateCourseSummary
import com.dulpick.app.domain.place.Coordinate
import com.dulpick.app.domain.place.Place
import com.dulpick.app.domain.place.PlaceOwnership
import com.dulpick.app.domain.place.SavedPlace
import kotlin.math.roundToInt

private const val SECONDS_PER_MINUTE = 60.0
private const val MAX_SEARCH_PAGE = 100

object CourseDtoMapper {

    fun toDomain(dto: DateCourseResponseDto): DateCourse {
        val sorted = dto.places.orEmpty().sortedBy { it.order }
        return DateCourse(
            id = dto.dateCourseId.toString(),
            title = dto.title,
            // 서버가 "2026-08-05" 를 타임존 없이 준다. 못 읽으면 틀린 날짜를 아래로 흘리지 않고 실패로 끝낸다
            scheduledDate = CourseDateFormat.date(dto.date) ?: throw CourseError.Unknown,
            // 시간 없는 코스는 null. 문자열이 있는데 못 읽으면 틀린 시간을 흘리지 않는다
            scheduledTime = dto.time?.let { CourseDateFormat.time(it) ?: throw CourseError.Unknown },
            status = status(dto.status),
            version = dto.version,
            stops = sorted.map { CourseStop(place = toPlace(it)) },
            // 구간은 항상 장소 수 - 1 개. 못 받은 구간은 null
            legs = sorted.dropLast(1).map { place -> place.walkToNext?.let { toLeg(it) } },
        )
    }

    private fun toLeg(dto: WalkToNextResponseDto): CourseLeg = CourseLeg(
        walkingMinutes = minutes(dto.durationSeconds),
        distanceMeters = dto.distanceMeters,
        walkingPath = dto.path.map { toCoordinate(it) },
        transit = dto.transit?.let { toTransit(it) },
    )

    // 대중교통 합계와, 구간들의 좌표를 이동 순서대로 이어 붙인 경로
    private fun toTransit(dto: TransitToNextResponseDto): CourseTransit = CourseTransit(
        minutes = minutes(dto.durationSeconds),
        distanceMeters = dto.distanceMeters,
        path = dto.legs.flatMap { it.path }.map { toCoordinate(it) },
    )

    private fun toCoordinate(dto: RoutePointResponseDto): Coordinate =
        Coordinate(dto.latitude, dto.longitude)

    // 서버는 초를 준다. 분으로 반올림한다. 도보·대중교통이 같은 방식이다
    private fun minutes(seconds: Int): Int = (seconds / SECONDS_PER_MINUTE).roundToInt()

    fun toSummary(dto: DateCourseSummaryResponseDto): DateCourseSummary =
        DateCourseSummary(
            id = dto.dateCourseId.toString(),
            title = dto.title,
            totalPlaceCount = dto.totalPlaceCount,
        )

    // 명세의 page 최대값이 100 이다. 101 을 요청하면 서버가 400 을 주므로 여기서 끊는다
    fun toSearchPage(dto: DateCoursePlaceSearchPageResponseDto): Page<SavedPlace> = Page(
        items = dto.places.map(::toSavedPlace),
        hasNext = dto.hasNext && dto.page < MAX_SEARCH_PAGE,
    )

    // 코스에 담을 후보. 저장 장소 응답과 거의 같아 SavedPlace 로 옮긴다.
    // 이 응답에 없는 카카오 번호·저장 수·저장 시각은 비워 둔다
    fun toSavedPlace(dto: DateCoursePlaceCandidateResponseDto): SavedPlace =
        SavedPlace(
            place = Place(
                id = dto.placeId.toString(),
                name = dto.name,
                category = PlaceCategoryMapper.fromCodeOrName(null, dto.categoryName),
                bookmarkCount = 0,
                thumbnailUrls = photoUrls(dto.thumbnailUrl, dto.imageUrls),
                kakaoPlaceId = null,
                address = dto.address,
                roadAddress = dto.roadAddress.orEmpty(),
                coordinate = Coordinate(dto.latitude, dto.longitude),
            ),
            ownership = ownership(dto.ownershipStatus),
            alias = dto.alias,
            savedAt = null,
        )

    private fun toPlace(dto: DateCoursePlaceResponseDto): Place =
        Place(
            id = dto.placeId.toString(),
            name = dto.name,
            category = PlaceCategoryMapper.fromCodeOrName(dto.category, dto.categoryName.orEmpty()),
            // 코스 응답에는 저장 수가 없다
            bookmarkCount = 0,
            thumbnailUrls = photoUrls(dto.thumbnailUrl, dto.imageUrls.orEmpty()),
            kakaoPlaceId = null,
            address = dto.address,
            roadAddress = dto.roadAddress.orEmpty(),
            coordinate = Coordinate(dto.latitude, dto.longitude),
        )

    // 대표 사진을 버리지 않되, 첫 이미지와 겹치면 한 번만 둔다 (iOS photoURLs 대응)
    private fun photoUrls(thumbnailUrl: String?, imageUrls: List<String>): List<String> =
        (listOfNotNull(thumbnailUrl) + imageUrls).distinct()

    // 모르는 값이 오면 DRAFT 로 둔다. 홈은 CONFIRMED 만 보므로 안 뜨는 쪽이 덜 위험하다
    private fun status(raw: String): CourseStatus =
        if (raw.uppercase() == "CONFIRMED") CourseStatus.CONFIRMED else CourseStatus.DRAFT

    // 아는 값만 그대로 옮긴다. 모르는 저장 관계는 내가 저장한 것으로 둔다 (iOS ?? .mine 대응)
    private fun ownership(raw: String): PlaceOwnership =
        when (raw.lowercase()) {
            "partner" -> PlaceOwnership.PARTNER
            "together" -> PlaceOwnership.TOGETHER
            else -> PlaceOwnership.MINE
        }
}
