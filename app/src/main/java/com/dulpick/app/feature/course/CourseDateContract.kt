package com.dulpick.app.feature.course

import com.dulpick.app.core.mvi.UiIntent
import com.dulpick.app.core.mvi.UiSideEffect
import com.dulpick.app.core.mvi.UiState
import java.time.LocalDate
import java.time.LocalTime

// 지금 열려 있는 휠이 무엇인지
enum class WheelTarget { DATE, TIME }

// 데이트 날짜 선택 화면. 날짜는 필수, 시간은 선택이다 (iOS CourseFeature 의 날짜 갈래 대응)
data class CourseDateState(
    val partnerNickname: String? = null,
    val date: LocalDate? = null,
    val time: LocalTime? = null,
    val showsDateError: Boolean = false,
    // null 이 아니면 휠 시트를 띄운다
    val activeWheel: WheelTarget? = null,
    // 시트 안에서 굴리는 임시값. 확인을 눌러야 date/time 으로 넘어간다
    val draftDate: LocalDate,
    val draftTime: LocalTime,
    // 자정을 넘겨도 하한과 초기값이 다른 날을 가리키지 않게, 한 번 센 내일로 둔다
    val tomorrow: LocalDate,
    val isCreatingCourse: Boolean = false,
) : UiState {
    // "2026.08.05"
    val dateText: String?
        get() = date?.let { "%04d.%02d.%02d".format(it.year, it.monthValue, it.dayOfMonth) }

    // "오후 1:00"
    val timeText: String?
        get() = time?.let {
            val isMorning = it.hour < NOON
            val hour12 = (it.hour % NOON).let { h -> if (h == 0) NOON else h }
            "%s %d:%02d".format(if (isMorning) "오전" else "오후", hour12, it.minute)
        }

    private companion object {
        const val NOON = 12
    }
}

sealed interface CourseDateIntent : UiIntent {
    data object OnAppear : CourseDateIntent
    data object DateFieldClicked : CourseDateIntent
    data object TimeFieldClicked : CourseDateIntent
    data class DraftDateChanged(val date: LocalDate) : CourseDateIntent
    data class DraftTimeChanged(val time: LocalTime) : CourseDateIntent
    data object WheelConfirmed : CourseDateIntent
    data object WheelDismissed : CourseDateIntent
    data object NextClicked : CourseDateIntent
    data object BackClicked : CourseDateIntent
}

sealed interface CourseDateSideEffect : UiSideEffect {
    // 코스를 만들었다. 다음 화면(장소 고르기)이 이 번호를 들고 간다
    data class PlacePickRequested(val dateCourseId: String, val version: Int) : CourseDateSideEffect
    data object Dismissed : CourseDateSideEffect
    data class ShowToast(val message: String) : CourseDateSideEffect
    data object SessionExpired : CourseDateSideEffect
}
