package com.dulpick.app.feature.placeimport

import androidx.lifecycle.viewModelScope
import com.dulpick.app.core.mvi.MviViewModel
import com.dulpick.app.domain.placeimport.ImportNextAction
import com.dulpick.app.domain.placeimport.ImportStatus
import com.dulpick.app.domain.placeimport.PlaceImport
import com.dulpick.app.domain.placeimport.PlaceImportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@Suppress("TooGenericExceptionCaught")
class PlaceImportViewModel @Inject constructor(
    private val repository: PlaceImportRepository,
) : MviViewModel<PlaceImportState, PlaceImportIntent, PlaceImportSideEffect>(PlaceImportState()) {

    private var importId: Long? = null
    private var started = false
    private var pollCount = 0
    private var job: Job? = null

    override fun onIntent(intent: PlaceImportIntent) {
        when (intent) {
            is PlaceImportIntent.Start -> start(intent.sourceUrl)
            is PlaceImportIntent.CandidateToggled -> toggle(intent.id)
            PlaceImportIntent.SaveClicked -> save()
            PlaceImportIntent.CloseClicked -> postSideEffect(PlaceImportSideEffect.Dismiss)
        }
    }

    // 첫 진입에만 추출을 시작한다. 같은 세션에서 재호출은 무시한다
    private fun start(sourceUrl: String) {
        if (started) return
        started = true
        job = viewModelScope.launch { runImport { repository.start(sourceUrl) } }
    }

    // 시작·폴링 공통: 결과로 다음 행동을 적용, 실패면 실패 화면. 취소는 그대로 전파한다
    private suspend fun runImport(block: suspend () -> PlaceImport) {
        try {
            applyImport(block())
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            fail()
        }
    }

    // 서버가 알려준 nextAction 에 따라 대기·후보표시·실패로 가른다 (iOS applyImport 대응)
    private fun applyImport(placeImport: PlaceImport) {
        importId = placeImport.importId
        when (placeImport.nextAction) {
            ImportNextAction.WAIT -> waitAndPoll(placeImport)
            ImportNextAction.SELECT_PLACES -> showCandidates(placeImport, failWhenEmpty = false)
            ImportNextAction.COMPLETED -> showCandidates(placeImport, failWhenEmpty = true)
            ImportNextAction.RETRY -> fail()
            // 명세에 없는 NONE 은 작업 상태로 판단한다
            ImportNextAction.NONE -> applyByStatus(placeImport)
        }
    }

    private fun applyByStatus(placeImport: PlaceImport) {
        when (placeImport.status) {
            ImportStatus.COMPLETED -> showCandidates(placeImport, failWhenEmpty = true)
            ImportStatus.REVIEW_REQUIRED -> showCandidates(placeImport, failWhenEmpty = false)
            ImportStatus.FAILED -> fail()
            ImportStatus.RECEIVED, ImportStatus.PROCESSING -> waitAndPoll(placeImport)
        }
    }

    // retryAfterSeconds(없으면 기본) 만큼 기다렸다 다시 폴링. 횟수 초과면 실패로 끊는다
    private fun waitAndPoll(placeImport: PlaceImport) {
        if (pollCount >= MAX_POLL_COUNT) {
            fail()
            return
        }
        pollCount += 1
        val seconds = placeImport.retryAfterSeconds?.takeIf { it > 0 } ?: FALLBACK_DELAY_SECONDS
        val id = placeImport.importId
        job = viewModelScope.launch {
            delay(seconds * MILLIS_PER_SECOND)
            runImport { repository.poll(id) }
        }
    }

    private fun showCandidates(placeImport: PlaceImport, failWhenEmpty: Boolean) {
        if (failWhenEmpty && placeImport.candidates.isEmpty()) {
            fail()
            return
        }
        setState {
            copy(
                phase = PlaceImportState.Phase.Loaded(placeImport),
                // 기본은 전부 선택
                selectedIds = placeImport.candidates.map { it.candidateId }.toSet(),
            )
        }
    }

    private fun fail() = setState { copy(phase = PlaceImportState.Phase.Failed) }

    private fun toggle(id: Long) {
        setState {
            copy(selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id)
        }
    }

    // 선택이 없으면 닫기, 있으면 저장 확정. 성공하면 닫고 홈이 재진입으로 갱신한다
    private fun save() {
        val ids = currentState.selectedIds
        if (ids.isEmpty()) {
            postSideEffect(PlaceImportSideEffect.Dismiss)
            return
        }
        val id = importId ?: return
        viewModelScope.launch {
            try {
                repository.confirm(id, ids.toList())
                postSideEffect(PlaceImportSideEffect.Dismiss)
            } catch (error: CancellationException) {
                throw error
            } catch (ignored: Throwable) {
                // iOS 처럼 저장 실패 시 화면을 유지한다
            }
        }
    }

    private companion object {
        const val MAX_POLL_COUNT = 7
        const val FALLBACK_DELAY_SECONDS = 2
        const val MILLIS_PER_SECOND = 1000L
    }
}
