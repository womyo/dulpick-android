package com.dulpick.app.feature.placedetail

import com.dulpick.app.core.mvi.UiIntent
import com.dulpick.app.core.mvi.UiSideEffect
import com.dulpick.app.core.mvi.UiState
import com.dulpick.app.domain.place.PlaceDetail

data class PlaceDetailState(
    val isLoading: Boolean = true,
    val isFailed: Boolean = false,
    val detail: PlaceDetail? = null,
    // 지번 주소 펼침 여부
    val isAddressExpanded: Boolean = false,
) : UiState {
    val title: String get() = detail?.place?.name.orEmpty()
}

sealed interface PlaceDetailIntent : UiIntent {
    data object OnAppear : PlaceDetailIntent
    data object AddressToggled : PlaceDetailIntent
    data object MapClicked : PlaceDetailIntent
    data object CloseClicked : PlaceDetailIntent
}

sealed interface PlaceDetailSideEffect : UiSideEffect {
    data object Close : PlaceDetailSideEffect
    // 카카오맵 앱/웹으로 열기 (앱 우선, 실패 시 웹)
    data class OpenKakaoMap(val appUri: String?, val webUrl: String?) : PlaceDetailSideEffect
    data object SessionExpired : PlaceDetailSideEffect
}
