package com.dulpick.app.data.course.mapper

import java.time.LocalDate
import java.time.LocalTime

// 보내는 형식과 받는 형식이 같은 약속이라 한 곳에 둔다 (iOS CourseDateFormat 대응)
object CourseDateFormat {
    // LocalDate -> "yyyy-MM-dd"
    fun dateText(date: LocalDate): String =
        "%04d-%02d-%02d".format(date.year, date.monthValue, date.dayOfMonth)

    // LocalTime -> "HH:mm:ss"
    fun timeText(time: LocalTime): String = "%02d:%02d:00".format(time.hour, time.minute)

    // "yyyy-MM-dd" -> LocalDate. 못 읽으면 null
    fun date(raw: String): LocalDate? = runCatching { LocalDate.parse(raw) }.getOrNull()

    // 못 읽은 조각을 떨어뜨리면 초가 분 자리로 올라온다.
    // 서버가 초 0 이면 초를 생략해 보내서 두 형식을 다 받는다
    fun time(raw: String): LocalTime? {
        val parts = raw.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull()?.takeIf { it in 0..MAX_HOUR }
        val minute = parts.getOrNull(1)?.toIntOrNull()?.takeIf { it in 0..MAX_MINUTE }
        if (hour == null || minute == null) return null
        return LocalTime.of(hour, minute)
    }

    private const val MAX_HOUR = 23
    private const val MAX_MINUTE = 59
}
