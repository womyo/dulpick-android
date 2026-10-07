package com.dulpick.app.feature.course

import androidx.lifecycle.viewModelScope
import com.dulpick.app.core.mvi.MviViewModel
import com.dulpick.app.domain.course.CourseError
import com.dulpick.app.domain.course.CourseRepository
import com.dulpick.app.domain.course.DateCourse
import com.dulpick.app.domain.course.DateCourseContent
import com.dulpick.app.ui.component.displayName
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
@Suppress("TooGenericExceptionCaught", "TooManyFunctions")
class CourseEditViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
) : MviViewModel<CourseEditState, CourseEditIntent, CourseEditSideEffect>(initialState()) {

    private var dateCourseId: String? = null
    private var saveJob: Job? = null

    override fun onIntent(intent: CourseEditIntent) {
        when (intent) {
            is CourseEditIntent.Start -> start(intent.dateCourseId)
            CourseEditIntent.RetryClicked -> load()
            is CourseEditIntent.TitleChanged -> setState { copy(title = intent.title) }
            CourseEditIntent.SaveClicked -> save()
            CourseEditIntent.BackClicked -> back()
            CourseEditIntent.BackModalClosed -> setState { copy(showsBackModal = false) }
            CourseEditIntent.BackModalDiscarded -> {
                setState { copy(showsBackModal = false) }
                postSideEffect(CourseEditSideEffect.Dismissed)
            }
            else -> onFormIntent(intent)
        }
    }

    // 휠 시트 인텐트 (onIntent 복잡도 분리)
    private fun onFormIntent(intent: CourseEditIntent) {
        when (intent) {
            CourseEditIntent.DateFieldClicked -> setState {
                copy(draftDate = scheduledDate ?: draftDate, activeWheel = WheelTarget.DATE)
            }
            // 기본값을 scheduledTime 에 넣지 않는다. 시간 없는 코스는 칸이 빈 채로 남는다
            CourseEditIntent.TimeFieldClicked -> setState {
                copy(draftTime = scheduledTime ?: draftTime, activeWheel = WheelTarget.TIME)
            }
            is CourseEditIntent.DraftDateChanged -> setState { copy(draftDate = intent.date) }
            is CourseEditIntent.DraftTimeChanged -> setState { copy(draftTime = intent.time) }
            CourseEditIntent.WheelConfirmed -> confirmWheel()
            CourseEditIntent.WheelDismissed -> setState { copy(activeWheel = null) }
            else -> onPlaceIntent(intent)
        }
    }

    // 장소 목록 인텐트
    private fun onPlaceIntent(intent: CourseEditIntent) {
        when (intent) {
            is CourseEditIntent.PlaceMoved -> move(intent.from, intent.to)
            is CourseEditIntent.PlaceDeleteClicked -> delete(intent.id)
            CourseEditIntent.UndoClicked -> undo()
            CourseEditIntent.ToastDismissed -> setState { copy(toast = null, pendingUndo = null) }
            CourseEditIntent.AddPlaceClicked ->
                postSideEffect(CourseEditSideEffect.AddPlaceRequested(currentState.placeIds))
            is CourseEditIntent.PlacesAdded -> add(intent.places)
            else -> Unit
        }
    }

    private fun start(id: String) {
        if (dateCourseId == id) return
        dateCourseId = id
        load()
    }

    private fun load() {
        val id = dateCourseId ?: return
        setState { copy(load = CourseEditLoad.LOADING) }
        viewModelScope.launch {
            try {
                applyLoaded(courseRepository.course(id))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                setState { copy(load = CourseEditLoad.FAILED) }
                if (error == CourseError.Unauthorized) {
                    postSideEffect(CourseEditSideEffect.SessionExpired)
                }
            }
        }
    }

    // 들어온 코스로 칸을 채우고, 바뀐 걸 가릴 기준도 같이 둔다
    private fun applyLoaded(course: DateCourse) {
        val places = course.stops.map {
            EditablePlace(
                id = it.place.id,
                name = it.place.name,
                category = it.place.category.displayName(),
                address = it.place.roadAddress.ifEmpty { it.place.address },
            )
        }
        setState {
            copy(
                load = CourseEditLoad.LOADED,
                version = course.version,
                title = course.title,
                scheduledDate = course.scheduledDate,
                scheduledTime = course.scheduledTime,
                places = places,
                entry = CourseEditSnapshot(
                    title = course.title,
                    date = course.scheduledDate,
                    time = course.scheduledTime,
                    placeIds = places.map { it.id },
                ),
            )
        }
    }

    private fun confirmWheel() {
        setState {
            when (activeWheel) {
                WheelTarget.DATE -> copy(scheduledDate = draftDate, activeWheel = null)
                WheelTarget.TIME -> copy(scheduledTime = draftTime, activeWheel = null)
                null -> this
            }
        }
    }

    private fun move(from: Int, to: Int) {
        setState {
            if (from !in places.indices) {
                this
            } else {
                val next = places.toMutableList()
                val moved = next.removeAt(from)
                next.add(minOf(to, next.size), moved)
                copy(places = next)
            }
        }
    }

    private fun delete(id: String) {
        val index = currentState.places.indexOfFirst { it.id == id }
        if (index < 0) return
        val removed = currentState.places[index]
        setState {
            copy(
                places = places - removed,
                pendingUndo = DeletedPlace(removed, index),
                toast = CourseEditToast("'${removed.name}' 삭제", showsUndo = true),
            )
        }
    }

    private fun undo() {
        val undo = currentState.pendingUndo ?: return
        setState {
            val next = places.toMutableList()
            next.add(minOf(undo.index, next.size), undo.place)
            copy(places = next, pendingUndo = null, toast = null)
        }
    }

    // 이미 담긴 곳은 다시 넣지 않는다. 고른 순서대로 뒤에 붙는다
    private fun add(added: List<EditablePlace>) {
        setState {
            val existing = places.map { it.id }.toSet()
            copy(places = places + added.filter { it.id !in existing })
        }
    }

    private fun back() {
        if (currentState.hasChanges) {
            setState { copy(showsBackModal = true) }
        } else {
            postSideEffect(CourseEditSideEffect.Dismissed)
        }
    }

    private fun save() {
        val id = dateCourseId ?: return
        val date = currentState.scheduledDate ?: return
        if (!currentState.canSave || saveJob?.isActive == true) return
        setState { copy(showsBackModal = false, isSaving = true) }
        val content = DateCourseContent(
            title = currentState.title,
            date = date,
            time = currentState.scheduledTime,
            placeIds = currentState.placeIds,
        )
        val version = currentState.version
        saveJob = viewModelScope.launch { runSave(id, content, version) }
    }

    private suspend fun runSave(id: String, content: DateCourseContent, version: Int) {
        try {
            // 응답이 최신 코스라 결과 화면이 서버를 다시 부르지 않아도 된다
            val course = courseRepository.updateCourse(id, content, version)
            setState { copy(isSaving = false) }
            postSideEffect(CourseEditSideEffect.Saved(course))
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            setState { copy(isSaving = false) }
            when (error) {
                CourseError.Unauthorized -> postSideEffect(CourseEditSideEffect.SessionExpired)
                // 상대가 먼저 고쳤다. 이 화면의 값으로 덮지 않고 결과 화면이 다시 읽게 한다
                CourseError.Conflict -> postSideEffect(CourseEditSideEffect.Conflicted)
                else -> setState {
                    copy(toast = CourseEditToast("저장하지 못했어요. 다시 시도해 주세요", showsUndo = false))
                }
            }
        }
    }

    private companion object {
        // 날짜 하한은 내일. 화면이 살아 있는 동안 자정을 넘겨도 바뀌지 않게 한 번만 센다
        fun initialState(): CourseEditState {
            val tomorrow = LocalDate.now().plusDays(1)
            return CourseEditState(
                draftDate = tomorrow,
                draftTime = LocalTime.now().withSecond(0).withNano(0),
                tomorrow = tomorrow,
            )
        }
    }
}
