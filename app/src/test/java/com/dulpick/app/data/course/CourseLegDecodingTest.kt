package com.dulpick.app.data.course

import com.dulpick.app.data.course.remote.dto.DateCourseResponseDto
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// 구간 응답의 경로·대중교통이 없거나 형식이 틀려도 코스 전체를 읽어야 한다.
// 네트워크 모듈과 같은 설정으로 읽는다 (iOS 코스 구간 디코딩 테스트 대응)
class CourseLegDecodingTest {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Test
    fun `경로 형식이 틀리면 빈 배열로 읽는다`() {
        val course = json.decodeFromString<DateCourseResponseDto>(course(walk = WALK_BROKEN_PATH))

        val walk = requireNotNull(course.places?.first()?.walkToNext)
        assertEquals(emptyList<Any>(), walk.path)
        assertEquals(1200, walk.distanceMeters)
    }

    @Test
    fun `대중교통 형식이 틀리면 null 로 읽는다`() {
        val course = json.decodeFromString<DateCourseResponseDto>(course(walk = WALK_BROKEN_TRANSIT))

        assertNull(requireNotNull(course.places?.first()?.walkToNext).transit)
    }

    @Test
    fun `경로와 대중교통 키가 없어도 도보 값은 읽는다`() {
        val course = json.decodeFromString<DateCourseResponseDto>(course(walk = WALK_MINIMAL))

        val walk = requireNotNull(course.places?.first()?.walkToNext)
        assertEquals(1200, walk.distanceMeters)
        assertEquals(1800, walk.durationSeconds)
        assertEquals(emptyList<Any>(), walk.path)
        assertNull(walk.transit)
    }

    @Test
    fun `제대로 온 경로와 대중교통은 그대로 읽는다`() {
        val course = json.decodeFromString<DateCourseResponseDto>(course(walk = WALK_FULL))

        val walk = requireNotNull(course.places?.first()?.walkToNext)
        assertEquals(1, walk.path.size)
        assertEquals(37.5, walk.path.first().latitude, 0.0001)
        val transit = requireNotNull(walk.transit)
        assertEquals(900, transit.durationSeconds)
        assertEquals(2, transit.legs.flatMap { it.path }.size)
    }

    // 대중교통 구간 하나의 좌표가 깨져도 나머지 구간은 읽는다
    @Test
    fun `대중교통 구간 하나의 경로가 깨져도 나머지는 읽는다`() {
        val course = json.decodeFromString<DateCourseResponseDto>(course(walk = WALK_BROKEN_LEG_PATH))

        val transit = requireNotNull(requireNotNull(course.places?.first()?.walkToNext).transit)
        assertEquals(listOf(0, 1), transit.legs.map { it.path.size })
    }

    private fun course(walk: String): String = """
        {
          "dateCourseId": 1,
          "title": "26.08.05 데이트",
          "date": "2026-08-05",
          "status": "CONFIRMED",
          "version": 3,
          "places": [
            {
              "order": 1,
              "placeId": 10,
              "name": "첫 장소",
              "address": "서울 중구",
              "latitude": 37.5,
              "longitude": 127.0,
              "walkToNext": $walk
            }
          ]
        }
    """.trimIndent()

    private companion object {
        const val WALK_MINIMAL = """{ "distanceMeters": 1200, "durationSeconds": 1800 }"""

        // path 가 배열이 아니라 문자열이다
        const val WALK_BROKEN_PATH =
            """{ "distanceMeters": 1200, "durationSeconds": 1800, "path": "없음" }"""

        // transit 이 객체가 아니라 숫자다
        const val WALK_BROKEN_TRANSIT =
            """{ "distanceMeters": 1200, "durationSeconds": 1800, "transit": 0 }"""

        const val WALK_FULL = """
            {
              "distanceMeters": 1200,
              "durationSeconds": 1800,
              "path": [{ "latitude": 37.5, "longitude": 127.0 }],
              "transit": {
                "distanceMeters": 3000,
                "durationSeconds": 900,
                "legs": [
                  { "path": [{ "latitude": 37.5, "longitude": 127.0 }] },
                  { "path": [{ "latitude": 37.6, "longitude": 127.1 }] }
                ]
              }
            }
        """

        const val WALK_BROKEN_LEG_PATH = """
            {
              "distanceMeters": 1200,
              "durationSeconds": 1800,
              "transit": {
                "distanceMeters": 3000,
                "durationSeconds": 900,
                "legs": [
                  { "path": { "latitude": 37.5 } },
                  { "path": [{ "latitude": 37.6, "longitude": 127.1 }] }
                ]
              }
            }
        """
    }
}
