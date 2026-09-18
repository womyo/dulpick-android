package com.dulpick.app.feature.placeimport

import com.dulpick.app.core.mvi.UiIntent
import com.dulpick.app.core.mvi.UiSideEffect
import com.dulpick.app.core.mvi.UiState
import com.dulpick.app.domain.placeimport.ImportCandidate
import com.dulpick.app.domain.placeimport.PlaceImport

data class PlaceImportState(
    val phase: Phase = Phase.Loading,
    val selectedIds: Set<Long> = emptySet(),
) : UiState {

    sealed interface Phase {
        // 추출 진행 중(시작·폴링)
        data object Loading : Phase
        // 후보가 준비돼 선택 가능
        data class Loaded(val placeImport: PlaceImport) : Phase
        // 추출 실패
        data object Failed : Phase
    }

    val candidates: List<ImportCandidate>
        get() = (phase as? Phase.Loaded)?.placeImport?.candidates ?: emptyList()

    val title: String
        get() = (phase as? Phase.Loaded)?.placeImport?.content?.title.orEmpty()

    val canonicalUrl: String
        get() = (phase as? Phase.Loaded)?.placeImport?.canonicalUrl.orEmpty()

    val isAllSelected: Boolean
        get() = candidates.isNotEmpty() && selectedIds.size == candidates.size

    // 선택이 없으면 닫기, 전부면 모두 저장, 일부면 N곳만 저장
    val saveButtonTitle: String
        get() = when {
            selectedIds.isEmpty() -> "닫기"
            isAllSelected -> "모두 저장"
            else -> "${selectedIds.size}곳만 저장"
        }
}

sealed interface PlaceImportIntent : UiIntent {
    // 공유로 받은 링크로 추출 시작
    data class Start(val sourceUrl: String) : PlaceImportIntent
    data class CandidateToggled(val id: Long) : PlaceImportIntent
    data object SaveClicked : PlaceImportIntent
    data object CloseClicked : PlaceImportIntent
}

sealed interface PlaceImportSideEffect : UiSideEffect {
    // 닫기(추출 실패·선택 없음·저장 완료). 홈은 재진입 onAppear 로 갱신된다
    data object Dismiss : PlaceImportSideEffect
}
