package com.dulpick.app.feature.course

import com.dulpick.app.core.mvi.UiIntent
import com.dulpick.app.core.mvi.UiSideEffect
import com.dulpick.app.core.mvi.UiState
import com.dulpick.app.domain.course.DateCourse
import java.time.LocalDate
import java.time.LocalTime

enum class CourseEditLoad { LOADING, LOADED, FAILED }

// 목록에 서는 장소 한 곳. 화면에 찍히는 글자만 담는다 (iOS EditablePlace 대응)
data class EditablePlace(
    val id: String,
    val name: String,
    val category: String,
    val address: String,
)

// 들어올 때의 값. 바뀐 게 있는지 이것과 비교해 가른다 (iOS Snapshot 대응)
data class CourseEditSnapshot(
    val title: String,
    val date: LocalDate,
    val time: LocalTime?,
    val placeIds: List<String>,
)

// 지운 장소와 그 자리. 실행취소가 같은 자리에 다시 넣는다
data class DeletedPlace(val place: EditablePlace, val index: Int)

// 이 화면의 토스트. 장소를 지웠을 때만 '실행취소' 가 붙는다
data class CourseEditToast(val message: String, val showsUndo: Boolean)

// 저장된 코스의 제목·날짜·시간·장소를 고치는 화면 (iOS CourseEditFeature 대응)
data class CourseEditState(
    val load: CourseEditLoad = CourseEditLoad.LOADING,
    val version: Int = 0,
    val title: String = "",
    val scheduledDate: LocalDate? = null,
    val scheduledTime: LocalTime? = null,
    val places: List<EditablePlace> = emptyList(),
    val entry: CourseEditSnapshot? = null,
    val pendingUndo: DeletedPlace? = null,
    // null 이 아니면 휠 시트를 띄운다
    val activeWheel: WheelTarget? = null,
    // 시트 안에서 굴리는 임시값. 확인을 눌러야 넘어간다
    val draftDate: LocalDate,
    val draftTime: LocalTime,
    // 자정을 넘겨도 하한이 다른 날을 가리키지 않게, 한 번 센 내일로 둔다
    val tomorrow: LocalDate,
    val showsBackModal: Boolean = false,
    val isSaving: Boolean = false,
    val toast: CourseEditToast? = null,
) : UiState {

    // 들어올 때와 다른 데가 하나라도 있는지. 뒤로 갈 때 저장을 물을지 가른다
    val hasChanges: Boolean
        get() {
            val entry = entry ?: return false
            return entry.title != title ||
                entry.date != scheduledDate ||
                entry.time != scheduledTime ||
                entry.placeIds != places.map { it.id }
        }

    val canSave: Boolean
        get() = load == CourseEditLoad.LOADED && !isSaving && places.isNotEmpty() && title.isNotBlank()

    // "2026.08.05"
    val dateText: String?
        get() = scheduledDate?.let { "%04d.%02d.%02d".format(it.year, it.monthValue, it.dayOfMonth) }

    // "오후 1:00"
    val timeText: String?
        get() = scheduledTime?.let {
            val isMorning = it.hour < NOON
            val hour12 = (it.hour % NOON).let { h -> if (h == 0) NOON else h }
            "%s %d:%02d".format(if (isMorning) "오전" else "오후", hour12, it.minute)
        }

    // 장소 추가 화면이 이미 담긴 곳을 목록에서 뺀다
    val placeIds: List<String> get() = places.map { it.id }

    private companion object {
        const val NOON = 12
    }
}

sealed interface CourseEditIntent : UiIntent {
    data class Start(val dateCourseId: String) : CourseEditIntent
    data object RetryClicked : CourseEditIntent
    data class TitleChanged(val title: String) : CourseEditIntent
    data object DateFieldClicked : CourseEditIntent
    data object TimeFieldClicked : CourseEditIntent
    data class DraftDateChanged(val date: LocalDate) : CourseEditIntent
    data class DraftTimeChanged(val time: LocalTime) : CourseEditIntent
    data object WheelConfirmed : CourseEditIntent
    data object WheelDismissed : CourseEditIntent
    data class PlaceMoved(val from: Int, val to: Int) : CourseEditIntent
    data class PlaceDeleteClicked(val id: String) : CourseEditIntent
    data object UndoClicked : CourseEditIntent
    data object ToastDismissed : CourseEditIntent
    data object AddPlaceClicked : CourseEditIntent
    // 장소 추가 화면에서 고른 장소들이 돌아왔다
    data class PlacesAdded(val places: List<EditablePlace>) : CourseEditIntent
    data object SaveClicked : CourseEditIntent
    data object BackClicked : CourseEditIntent
    data object BackModalClosed : CourseEditIntent
    data object BackModalDiscarded : CourseEditIntent
}

sealed interface CourseEditSideEffect : UiSideEffect {
    // 이미 담긴 장소를 빼고 고르게 한다
    data class AddPlaceRequested(val excluding: List<String>) : CourseEditSideEffect
    data class Saved(val course: DateCourse) : CourseEditSideEffect
    // 상대가 먼저 고쳤다. 결과 화면이 다시 불러와야 한다
    data object Conflicted : CourseEditSideEffect
    data object Dismissed : CourseEditSideEffect
    data object SessionExpired : CourseEditSideEffect
}
