package com.dulpick.app.feature.course

import com.dulpick.app.core.mvi.UiIntent
import com.dulpick.app.core.mvi.UiSideEffect
import com.dulpick.app.core.mvi.UiState
import com.dulpick.app.core.map.MapRoute
import com.dulpick.app.domain.course.CourseStop
import com.dulpick.app.domain.course.CourseTravelMode
import com.dulpick.app.domain.course.DateCourse
import com.dulpick.app.domain.place.Coordinate
import com.dulpick.app.ui.component.displayName
import kotlin.math.roundToInt

enum class CourseResultLoad { LOADING, LOADED, FAILED }

// 이 화면에 어디서 들어왔는지. 지난 데이트면 수정·알리기를 안 낸다.
// status 로 대신하면 안 된다 — 지난 데이트도 CONFIRMED 라 예정 데이트와 구별이 안 된다
enum class CourseResultOrigin { COURSE_BUILT, PAST_DATE }

// 수정 화면에서 돌아온 이유. 충돌이면 최신 코스를 읽었다고 알린다
enum class CourseReloadReason { SAVED, CONFLICT }

data class TimelineStop(
    val id: String,
    val name: String,
    val category: String,
    val address: String,
)

// 값을 못 받은 구간은 시간을 비우고 거리 자리에 안내 문구가 온다
data class TimelineLeg(
    val mode: TimelineLegMode,
    val duration: String,
    val distance: String,
)

enum class TimelineLegMode { WALK, TRANSIT, UNAVAILABLE }

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

    // 구간 행에 보이는 값의 합이라 사용자가 화면에서 직접 맞춰 볼 수 있다
    val summaryText: String?
        get() {
            val course = course ?: return null
            if (course.stops.isEmpty()) return null
            val legs = course.legs.filterNotNull()
            val duration = durationText(legs.sumOf { it.travelMinutes })
            val distance = kilometers(legs.sumOf { tenthsOfKilometer(it.travelDistanceMeters) })
            // 대중교통이 섞인 시간을 「도보」 라 부르지 않고, 「이동」 을 한 줄에 두 번 쓰지 않는다
            return if (legs.none { it.travelMode == CourseTravelMode.TRANSIT }) {
                "${course.stops.size}곳 · 도보 약 $duration · 총 이동 ${distance}km"
            } else {
                "${course.stops.size}곳 · 이동 약 $duration · 총 ${distance}km"
            }
        }

    // 장소 → 구간 경로 → 다음 장소. 경로를 못 받은 구간은 장소끼리 직선이다
    val routeCoordinates: List<Coordinate>
        get() {
            val legs = course?.legs.orEmpty()
            return stops.flatMapIndexed { index, stop ->
                val leg = legs.getOrNull(index)
                if (leg == null) listOf(stop.place.coordinate) else listOf(stop.place.coordinate) + leg.travelPath
            }
        }

    val routes: List<MapRoute>
        get() = if (stops.size >= 2) {
            listOf(MapRoute("course-${course?.id}", routeCoordinates))
        } else {
            emptyList()
        }

    // 구간 수(stops.size - 1)만큼. 값을 못 받은 구간은 너무 멀어서 빈 경우가 대부분이라
    // 도보 대신 대중교통 길찾기로 연다
    val legDirections: List<CourseDirections>
        get() {
            if (stops.size < 2) return emptyList()
            val legs = course?.legs.orEmpty()
            return (0 until stops.size - 1).map { index ->
                CourseDirections.of(
                    origin = stops[index].place,
                    destination = stops[index + 1].place,
                    mode = legs.getOrNull(index)?.travelMode ?: CourseTravelMode.TRANSIT,
                )
            }
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
                TimelineLeg(TimelineLegMode.UNAVAILABLE, duration = "", distance = "이동 정보를 불러오지 못했어요")
            } else {
                val duration = durationText(leg.travelMinutes)
                val distance = "${kilometers(tenthsOfKilometer(leg.travelDistanceMeters))} km"
                when (leg.travelMode) {
                    CourseTravelMode.WALKING -> TimelineLeg(TimelineLegMode.WALK, "도보 $duration", distance)
                    CourseTravelMode.TRANSIT -> TimelineLeg(TimelineLegMode.TRANSIT, "대중교통 $duration", distance)
                }
            }
        }

    private companion object {
        const val METERS_PER_DECIKM = 100.0
        const val MINUTES_PER_HOUR = 60

        // 행과 요약이 0.1km 단위 하나를 함께 써야 요약이 행의 합과 맞는다
        fun tenthsOfKilometer(meters: Int): Int = (meters / METERS_PER_DECIKM).roundToInt()

        fun kilometers(tenths: Int): String = "${tenths / 10}.${tenths % 10}"

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
    // 상대가 먼저 바꿔 저장이 막혔다. 최신 코스를 읽고 그 사실을 알린다
    data object ConflictReloadRequested : CourseResultIntent
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
