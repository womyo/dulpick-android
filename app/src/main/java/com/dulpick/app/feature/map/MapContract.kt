package com.dulpick.app.feature.map

import com.dulpick.app.core.mvi.UiIntent
import com.dulpick.app.core.mvi.UiSideEffect
import com.dulpick.app.core.mvi.UiState
import com.dulpick.app.domain.place.PlaceCategory
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
    // null 이면 전체 카테고리
    val selectedCategory: PlaceCategory? = null,
    // null 이 아니면 별칭 편집 시트를 띄운다
    val aliasEdit: AliasEdit? = null,
) : UiState {
    // 지도 핀과 시트 목록이 함께 보는 하나의 배열. 소유자·카테고리 필터를 적용한다
    val filteredPlaces: List<SavedPlace>
        get() = places
            .filter { selectedOwnership.matches(it.ownership) }
            .filter { selectedCategory == null || it.place.category == selectedCategory }

    // 저장한 장소 자체가 없다(필터 때문에 빈 것과 문구가 다르다)
    val hasNoSavedPlace: Boolean get() = !isLoading && places.isEmpty()

    // 다 불러온 뒤 보일 게 없다(필터 결과 포함)
    val isEmpty: Boolean get() = !isLoading && filteredPlaces.isEmpty()
}

// 별칭 편집 시트 상태 (iOS PlaceAliasFeature.State 대응)
data class AliasEdit(
    val placeId: String,
    val placeName: String,
    val address: String,
    val initialAlias: String,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
) {
    companion object {
        const val MAX_ALIAS_LENGTH = 15
    }
}

sealed interface MapIntent : UiIntent {
    data object OnAppear : MapIntent
    data class OwnershipSelected(val ownership: PlaceOwnership) : MapIntent
    // null 이면 전체
    data class CategorySelected(val category: PlaceCategory?) : MapIntent
    data class DeleteClicked(val id: String) : MapIntent
    data class EditClicked(val id: String) : MapIntent
    data class AliasSaveClicked(val alias: String) : MapIntent
    data object AliasEditDismissed : MapIntent
}

sealed interface MapSideEffect : UiSideEffect {
    data object SessionExpired : MapSideEffect
    // isError 면 에러 아이콘이 붙은 토스트로 띄운다 (iOS ToastState.error 대응)
    data class ShowToast(val message: String, val isError: Boolean) : MapSideEffect
}
