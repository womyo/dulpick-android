package com.dulpick.app.feature.map

import com.dulpick.app.core.mvi.UiIntent
import com.dulpick.app.core.mvi.UiSideEffect
import com.dulpick.app.core.mvi.UiState
import com.dulpick.app.domain.place.SavedPlace

data class MapState(
    val places: List<SavedPlace> = emptyList(),
    // 저장 장소를 아직 못 불러온 초기 상태
    val isLoading: Boolean = true,
) : UiState

sealed interface MapIntent : UiIntent {
    data object OnAppear : MapIntent
}

sealed interface MapSideEffect : UiSideEffect {
    data object SessionExpired : MapSideEffect
}
