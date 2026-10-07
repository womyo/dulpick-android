package com.dulpick.app.feature.course.component

import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

// 오전·오후 (iOS DayPeriod 대응)
enum class DayPeriod(val title: String) { AM("오전"), PM("오후") }

// 날짜 휠 세 열의 보이는 범위와, 고른 값이 그 밖으로 나갔을 때 되돌릴 자리를 계산한다.
// minimum 이 있으면 그보다 앞선 연·월·일은 목록에서 빠진다 (iOS WheelDateRange 대응)
data class WheelDateRange(
    val yearRange: IntRange,
    val minimum: LocalDate? = null,
) {
    val years: List<Int> get() = (yearLowerBound..yearRange.last).toList()

    fun months(year: Int): List<Int> = (monthLowerBound(year)..MONTHS_IN_YEAR).toList()

    fun days(year: Int, month: Int): List<Int> {
        val last = YearMonth.of(year, month).lengthOfMonth()
        val first = minOf(dayLowerBound(year, month), last)
        return (first..last).toList()
    }

    // 범위 밖 값을 범위 안으로 끌어온다
    fun resolved(selection: LocalDate): LocalDate {
        val year = selection.year.coerceIn(yearLowerBound, yearRange.last)
        val monthStart = monthLowerBound(year)
        val month = selection.monthValue.coerceIn(monthStart, MONTHS_IN_YEAR)
        val lastDay = YearMonth.of(year, month).lengthOfMonth()
        val dayStart = dayLowerBound(year, month)
        // 하한으로 올린 뒤 그 달 일수로 내린다. 나중에 올리면 그 달에 없는 날이 될 수 있다
        val day = selection.dayOfMonth.coerceAtLeast(dayStart).coerceAtMost(lastDay)
        return LocalDate.of(year, month, day)
    }

    private val yearLowerBound: Int
        get() = minimum?.year?.coerceIn(yearRange.first, yearRange.last) ?: yearRange.first

    private fun monthLowerBound(year: Int): Int =
        if (minimum != null && year == minimum.year) minimum.monthValue else 1

    private fun dayLowerBound(year: Int, month: Int): Int =
        if (minimum != null && year == minimum.year && month == minimum.monthValue) {
            minimum.dayOfMonth
        } else {
            1
        }

    private companion object {
        const val MONTHS_IN_YEAR = 12
    }
}

// 시각 휠 세 열의 보이는 범위. 시 열은 12시간제([12] + 1..11)고 resolved 의 hour 는 0..23 이다
// (iOS WheelTimeRange 대응)
data class WheelTimeRange(
    val minuteStep: Int,
    val minimum: LocalTime? = null,
) {
    val periods: List<DayPeriod>
        get() = when {
            minimum == null -> DayPeriod.entries
            minimum.hour < NOON -> DayPeriod.entries
            else -> listOf(DayPeriod.PM)
        }

    fun hours(period: DayPeriod): List<Int> {
        val minimumHour = minimum?.hour ?: return HOURS
        val minimumPeriod = if (minimumHour < NOON) DayPeriod.AM else DayPeriod.PM
        if (period != minimumPeriod) return HOURS
        val index = HOURS.indexOf(hour12(minimumHour))
        return if (index < 0) HOURS else HOURS.drop(index)
    }

    fun minutes(period: DayPeriod, hour: Int): List<Int> {
        val start = minuteLowerBound(hour24(period, hour))
        return allMinutes().filter { it >= start }
    }

    fun resolved(selection: LocalTime): LocalTime {
        val hourStart = minimum?.hour ?: 0
        val hour = selection.hour.coerceIn(hourStart, MAX_HOUR)
        val minuteStart = minuteLowerBound(hour)
        return LocalTime.of(hour, snappedMinute(selection.minute, minuteStart))
    }

    fun hour12(hour24: Int): Int = (hour24 % NOON).let { if (it == 0) NOON else it }

    fun hour24(period: DayPeriod, hour12: Int): Int =
        (hour12 % NOON) + if (period == DayPeriod.PM) NOON else 0

    fun period(hour24: Int): DayPeriod = if (hour24 < NOON) DayPeriod.AM else DayPeriod.PM

    private val resolvedStep: Int get() = if (minuteStep in 1..MINUTES_IN_HOUR) minuteStep else 1

    private fun allMinutes(): List<Int> = (0 until MINUTES_IN_HOUR step resolvedStep).toList()

    private fun minuteLowerBound(hour24: Int): Int =
        if (minimum != null && hour24 == minimum.hour) minimum.minute else 0

    // 간격에 맞는 값 중 고른 값을 넘지 않는 가장 큰 것
    private fun snappedMinute(minute: Int, lowerBound: Int): Int {
        val allowed = allMinutes().filter { it >= lowerBound }
        val clamped = minute.coerceIn(lowerBound, MAX_MINUTE)
        return allowed.lastOrNull { it <= clamped } ?: allowed.firstOrNull() ?: lowerBound
    }

    private companion object {
        val HOURS = listOf(12) + (1..11).toList()
        const val NOON = 12
        const val MAX_HOUR = 23
        const val MAX_MINUTE = 59
        const val MINUTES_IN_HOUR = 60
    }
}
