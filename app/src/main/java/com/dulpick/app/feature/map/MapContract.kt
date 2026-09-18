package com.dulpick.app.feature.map

import com.dulpick.app.core.mvi.UiIntent
import com.dulpick.app.core.mvi.UiSideEffect
import com.dulpick.app.core.mvi.UiState
import com.dulpick.app.domain.place.PlaceOwnership
import com.dulpick.app.domain.place.SavedPlace
import com.dulpick.app.domain.place.matches

data class MapState(
    val places: List<SavedPlace> = emptyList(),
    // 저장 장소를 아직 못 불러온 초기 상태
    val isLoading: Boolean = true,
    // 커플 연동 시에만 소유자 필터를 노출한다
    val isCoupleConnected: Boolean = false,
    val selectedOwnership: PlaceOwnership = PlaceOwnership.TOGETHER,
) : UiState {
    // 지도 핀과 시트 목록이 함께 보는 하나의 배열. 소유자 필터를 적용한다
    val filteredPlaces: List<SavedPlace>
        get() = places.filter { selectedOwnership.matches(it.ownership) }

    // 저장한 장소 자체가 없다(필터 때문에 빈 것과 문구가 다르다)
    val hasNoSavedPlace: Boolean get() = !isLoading && places.isEmpty()

    // 다 불러온 뒤 보일 게 없다(필터 결과 포함)
    val isEmpty: Boolean get() = !isLoading && filteredPlaces.isEmpty()
}

sealed interface MapIntent : UiIntent {
    data object OnAppear : MapIntent
    data class OwnershipSelected(val ownership: PlaceOwnership) : MapIntent
}

sealed interface MapSideEffect : UiSideEffect {
    data object SessionExpired : MapSideEffect
}
