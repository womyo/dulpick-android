package com.dulpick.app.domain.course

import com.dulpick.app.domain.place.Place
import java.time.LocalDate
import java.time.LocalTime

// 코스 상태. DRAFT 는 쓰다 만 코스, CONFIRMED 는 확정된 데이트다.
// 홈의 다가오는·지난 데이트는 확정된 코스만 본다 (iOS CourseStatus 대응)
enum class CourseStatus { DRAFT, CONFIRMED }

// 구간 이동 값. 서버가 계산해서 내려준다. 클라이언트는 추정하지 않는다 (iOS CourseLeg 대응)
data class CourseLeg(
    val walkingMinutes: Int,
    val distanceMeters: Int,
)

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
) {
    val totalWalkingMinutes: Int get() = legs.filterNotNull().sumOf { it.walkingMinutes }
    val totalDistanceMeters: Int get() = legs.filterNotNull().sumOf { it.distanceMeters }
}

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
