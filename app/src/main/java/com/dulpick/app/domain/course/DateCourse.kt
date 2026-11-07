package com.dulpick.app.domain.course

import com.dulpick.app.domain.place.Coordinate
import com.dulpick.app.domain.place.Place
import java.time.LocalDate
import java.time.LocalTime

// 코스 상태. DRAFT 는 쓰다 만 코스, CONFIRMED 는 확정된 데이트다.
// 홈의 다가오는·지난 데이트는 확정된 코스만 본다 (iOS CourseStatus 대응)
enum class CourseStatus { DRAFT, CONFIRMED }

// 구간을 무엇으로 이동하는지. 버스·지하철·둘 다 TRANSIT 하나다 (iOS CourseTravelMode 대응)
enum class CourseTravelMode { WALKING, TRANSIT }

// 서버가 도보 30분 이상 구간에만 함께 주는 대중교통 경로의 합계.
// 요금·환승·정류장·노선은 화면이 안 쓰므로 담지 않는다 (iOS CourseTransit 대응)
data class CourseTransit(
    val minutes: Int,
    val distanceMeters: Int,
    // 대중교통 구간들의 좌표를 이동 순서대로 이어 붙인 것. 서버가 좌표를 못 주면 빈 배열
    val path: List<Coordinate> = emptyList(),
)

// 구간 이동 값. 서버가 계산해서 내려준다. 클라이언트는 추정하지 않는다.
//
// 서버의 도보 값·도보 경로·대중교통 값을 모두 담는다.
// 화면에 보일 값은 travel* 프로퍼티가 정한다. 대중교통이 있으면 대중교통이다 (iOS CourseLeg 대응)
data class CourseLeg(
    val walkingMinutes: Int,
    // 도보 거리
    val distanceMeters: Int,
    // 도보 경로 좌표. 서버가 못 주면 빈 배열
    val walkingPath: List<Coordinate> = emptyList(),
    // 도보 30분 이상 구간에만 온다. 해당하지 않거나 조회에 실패하면 null
    val transit: CourseTransit? = null,
) {
    // 서버는 대중교통을 걷는 것보다 나은 구간에만 준다. 그래서 오면 대중교통만 보인다
    val travelMode: CourseTravelMode
        get() = if (transit == null) CourseTravelMode.WALKING else CourseTravelMode.TRANSIT

    val travelMinutes: Int get() = transit?.minutes ?: walkingMinutes

    val travelDistanceMeters: Int get() = transit?.distanceMeters ?: distanceMeters

    // 대중교통 경로가 비어도 도보 경로로 바꾸지 않는다. 행의 수단과 다른 길을 그리지 않는다
    val travelPath: List<Coordinate> get() = transit?.path ?: walkingPath
}

// 코스에 담긴 장소 한 곳 (iOS CourseStop 대응)
data class CourseStop(val place: Place) {
    val id: String get() = place.id
}

data class DateCourse(
    val id: String,
    // 시안 "26.08.05 데이트"
    val title: String,
    val scheduledDate: LocalDate,
    // 날짜만 저장된 코스면 null
    val scheduledTime: LocalTime?,
    val status: CourseStatus,
    // 낙관적 락 번호. 클라이언트가 세지 않고 서버 응답 값을 그대로 들고 다닌다
    val version: Int,
    // 방문 순서대로. 번호 배지가 이 순서를 따른다
    val stops: List<CourseStop>,
    // 서버가 계산해 보낸 구간 값. 항상 stops.size - 1 개. 못 받은 구간은 null
    val legs: List<CourseLeg?>,
)

// 코스 저장에 실리는 제목·날짜·시간·장소. 어느 코스의 몇 번째 판인지는 안 담는다
data class DateCourseContent(
    val title: String,
    val date: LocalDate,
    val time: LocalTime?,
    val placeIds: List<String>,
)

// 데이트명은 사용자가 날짜 화면에서 입력하지 않는다.
// 시안 코스 결과 헤더가 "26.08.05 데이트" 라서 날짜에서 만들어 서버로 보낸다
object DateCourseTitle {
    fun make(date: LocalDate): String =
        "%02d.%02d.%02d 데이트".format(date.year % 100, date.monthValue, date.dayOfMonth)
}
