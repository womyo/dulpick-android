package com.dulpick.app.feature.course.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dulpick.app.ui.theme.Colors
import com.dulpick.app.ui.theme.Typography
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

// 열 3개 오른쪽 안쪽 여백
private val TRAILING_INSET = 12.dp

// 열 가운데에서 단위 상자 가운데까지 (iOS unitCenterOffset)
private val UNIT_CENTER_OFFSET = 27.5.dp

// 연 / 월 / 일 3열 휠. OS 휠이 아니라 평평한 스크롤 목록이다.
// 선택 바는 세 열을 가로지르는 사각 하나이고 열 뒤층에 깔린다.
// 단위 "월"·"일" 은 휠 항목이 아니라 선택 바 위에 고정된 별도 층이라 늘 보인다 (iOS DateWheelPicker 대응)
@Composable
fun DateWheelPicker(
    selection: LocalDate,
    onSelect: (LocalDate) -> Unit,
    range: WheelDateRange,
    modifier: Modifier = Modifier,
) {
    val resolved = range.resolved(selection)
    // 일 열 행마다 달력을 돌지 않게, 이번 그리기에서 한 번만 센다
    val days = range.days(resolved.year, resolved.monthValue)

    Box(
        modifier = modifier.fillMaxWidth().height(WheelMetrics.areaHeight),
        contentAlignment = Alignment.Center,
    ) {
        WheelSelectionBar()
        Row(
            horizontalArrangement = Arrangement.spacedBy(WheelMetrics.columnSpacing),
            modifier = Modifier.padding(end = TRAILING_INSET),
        ) {
            WheelColumn(
                items = range.years,
                selected = resolved.year,
                onSelect = { onSelect(range.resolved(resolved.withYearSafely(it))) },
                title = { it.toString() },
            )
            WheelColumn(
                items = range.months(resolved.year),
                selected = resolved.monthValue,
                onSelect = { onSelect(range.resolved(resolved.withMonthSafely(it))) },
                title = ::twoDigits,
            )
            WheelColumn(
                items = days,
                selected = resolved.dayOfMonth,
                onSelect = { onSelect(range.resolved(resolved.withDayOfMonth(it))) },
                title = ::twoDigits,
            )
        }
        UnitOverlay()
    }
}

// 열과 같은 가로 배치를 한 번 더 깔아 단위를 각 열 가운데 기준으로 세운다. 숫자는 단위 때문에 밀리지 않는다
@Composable
private fun UnitOverlay() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(WheelMetrics.columnSpacing),
        modifier = Modifier.padding(end = TRAILING_INSET),
    ) {
        UnitSlot(null)
        UnitSlot("월")
        UnitSlot("일")
    }
}

@Composable
private fun UnitSlot(unit: String?) {
    Box(
        modifier = Modifier.size(width = WheelMetrics.columnWidth, height = WheelMetrics.rowHeight),
        contentAlignment = Alignment.Center,
    ) {
        if (unit != null) {
            Text(
                text = unit,
                style = Typography.body1M,
                color = Colors.textSecondary,
                maxLines = 1,
                modifier = Modifier.offset(x = UNIT_CENTER_OFFSET),
            )
        }
    }
}

// 오전·오후 / 시 / 분 3열 휠. 구조는 날짜 휠과 같고 단위 글자와 안쪽 여백이 없다
// (iOS TimeWheelPicker 대응)
@Composable
fun TimeWheelPicker(
    selection: LocalTime,
    onSelect: (LocalTime) -> Unit,
    range: WheelTimeRange,
    modifier: Modifier = Modifier,
) {
    val resolved = range.resolved(selection)
    val period = range.period(resolved.hour)
    val hour12 = range.hour12(resolved.hour)

    Box(
        modifier = modifier.fillMaxWidth().height(WheelMetrics.areaHeight),
        contentAlignment = Alignment.Center,
    ) {
        WheelSelectionBar()
        Row(horizontalArrangement = Arrangement.spacedBy(WheelMetrics.columnSpacing)) {
            WheelColumn(
                items = range.periods,
                selected = period,
                onSelect = { onSelect(range.at(it, hour12, resolved.minute)) },
                title = { it.title },
            )
            WheelColumn(
                items = range.hours(period),
                selected = hour12,
                onSelect = { onSelect(range.at(period, it, resolved.minute)) },
                title = ::twoDigits,
            )
            WheelColumn(
                items = range.minutes(period, hour12),
                selected = resolved.minute,
                onSelect = { onSelect(range.at(period, hour12, it)) },
                title = ::twoDigits,
            )
        }
    }
}

// 세 열이 고른 값을 하나의 시각으로 합친다
private fun WheelTimeRange.at(period: DayPeriod, hour12: Int, minute: Int): LocalTime =
    resolved(LocalTime.of(hour24(period, hour12), minute))

// 연·월만 바꾸면 그 달에 없는 날이 될 수 있어 일수로 먼저 내린다
private fun LocalDate.withYearSafely(year: Int): LocalDate =
    withDayOfMonth(minOf(dayOfMonth, YearMonth.of(year, monthValue).lengthOfMonth())).withYear(year)

private fun LocalDate.withMonthSafely(month: Int): LocalDate =
    withDayOfMonth(minOf(dayOfMonth, YearMonth.of(year, month).lengthOfMonth())).withMonth(month)
