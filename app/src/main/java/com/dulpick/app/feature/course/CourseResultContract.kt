package com.dulpick.app.feature.course

import com.dulpick.app.core.mvi.UiIntent
import com.dulpick.app.core.mvi.UiSideEffect
import com.dulpick.app.core.mvi.UiState
import com.dulpick.app.domain.course.CourseStop
import com.dulpick.app.domain.course.DateCourse
import com.dulpick.app.feature.map.component.displayName

// 이 분을 넘는 구간에 "이동이 긴 구간입니다" 를 붙인다 (iOS longLegMinutes)
const val LONG_LEG_MINUTES = 30

enum class CourseResultLoad { LOADING, LOADED, FAILED }

// 이 화면에 어디서 들어왔는지. 지난 데이트면 수정·알리기를 안 낸다.
// status 로 대신하면 안 된다 — 지난 데이트도 CONFIRMED 라 예정 데이트와 구별이 안 된다
enum class CourseResultOrigin { COURSE_BUILT, PAST_DATE }

// 타임라인 한 줄. 화면에 그대로 찍히는 글자만 담는다 (iOS CourseStop 뷰 모델 대응)
data class TimelineStop(
    val id: String,
    val name: String,
    val category: String,
    val address: String,
)

// 두 곳 사이 이동. 시간·거리는 이미 만들어진 글자다 (iOS CourseLeg 뷰 모델 대응)
data class TimelineLeg(
    val duration: String,
    val distance: String,
    val isLong: Boolean,
)

data class CourseResultState(
    val course: DateCourse? = null,
    val partnerNickname: String? = null,
    val origin: CourseResultOrigin = CourseResultOrigin.COURSE_BUILT,
    val load: CourseResultLoad = CourseResultLoad.LOADING,
    val isNotifyingPartner: Boolean = false,
) : UiState {
    val stops: List<CourseStop> get() = course?.stops.orEmpty()

    // 지난 데이트는 고칠 이유가 없다. 서버는 안 막으니 화면이 막는다
    val showsEditButton: Boolean get() = origin != CourseResultOrigin.PAST_DATE

    // 장소가 없으면 알릴 것이 없고, 지난 데이트는 알릴 이유가 없다
    val showsNotifyButton: Boolean
        get() = origin != CourseResultOrigin.PAST_DATE && stops.isNotEmpty()

    val notifyTitle: String
        get() = partnerNickname?.let { "${it}에게 코스 알리기" } ?: "상대에게 코스 알리기"

    // 장소가 없으면 요약 줄이 없다
    val summaryText: String?
        get() {
            val course = course ?: return null
            if (course.stops.isEmpty()) return null
            val duration = durationText(course.totalWalkingMinutes)
            val distance = "%.1fkm".format(course.totalDistanceMeters / METERS_PER_KM)
            return "${course.stops.size}곳 · 도보 약 $duration · 총 이동 $distance"
        }

    val timelineStops: List<TimelineStop>
        get() = stops.map {
            TimelineStop(
                id = it.id,
                name = it.place.name,
                category = it.place.category.displayName(),
                address = it.place.roadAddress.ifEmpty { it.place.address },
            )
        }

    val timelineLegs: List<TimelineLeg>
        get() = course?.legs.orEmpty().map { leg ->
            if (leg == null) {
                TimelineLeg(duration = "", distance = "", isLong = false)
            } else {
                TimelineLeg(
                    duration = "도보 ${durationText(leg.walkingMinutes)}",
                    distance = "%.1f km".format(leg.distanceMeters / METERS_PER_KM),
                    isLong = leg.walkingMinutes > LONG_LEG_MINUTES,
                )
            }
        }

    private companion object {
        const val METERS_PER_KM = 1_000.0
        const val MINUTES_PER_HOUR = 60

        fun durationText(minutes: Int): String {
            val hours = minutes / MINUTES_PER_HOUR
            val remain = minutes % MINUTES_PER_HOUR
            return when {
                hours > 0 && remain > 0 -> "${hours}시간 ${remain}분"
                hours > 0 -> "${hours}시간"
                else -> "${minutes}분"
            }
        }
    }
}

sealed interface CourseResultIntent : UiIntent {
    data class Start(val dateCourseId: String, val origin: CourseResultOrigin) : CourseResultIntent
    data object RetryClicked : CourseResultIntent
    data object NotifyClicked : CourseResultIntent
    data object EditClicked : CourseResultIntent
    data object BackClicked : CourseResultIntent
}

sealed interface CourseResultSideEffect : UiSideEffect {
    data class EditRequested(val dateCourseId: String) : CourseResultSideEffect
    data object Dismissed : CourseResultSideEffect
    data class ShowToast(val message: String) : CourseResultSideEffect
    data object SessionExpired : CourseResultSideEffect
}
