package com.dulpick.app.domain.course

import com.dulpick.app.domain.place.SavedPlace
import java.time.LocalDate
import java.time.LocalTime

// Feature 는 이 인터페이스로만 코스 데이터에 접근한다 (iOS CourseClient 대응).
// 지난 데이트 목록(GET /date-courses/past)은 홈 쪽 통로가 이미 쓰고 있어 여기에 두지 않는다
interface CourseRepository {
    // 데이트명·날짜·시간만 보낸다. 장소는 안 보낸다. 응답은 DRAFT 코스다
    suspend fun createCourse(title: String, date: LocalDate, time: LocalTime?): DateCourse

    // 코스에 담을 수 있는 저장 장소
    suspend fun coursePlaces(): List<SavedPlace>

    suspend fun course(id: String): DateCourse

    // 다가오는 데이트. 없으면 null
    suspend fun currentCourse(): DateCourseSummary?

    // 확정 저장. version 이 어긋나면 Conflict 다
    suspend fun updateCourse(id: String, content: DateCourseContent, version: Int): DateCourse

    suspend fun notifyPartner(id: String)
}
