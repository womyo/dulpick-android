package com.dulpick.app.feature.course

import com.dulpick.app.core.mvi.UiIntent
import com.dulpick.app.core.mvi.UiSideEffect
import com.dulpick.app.core.mvi.UiState
import com.dulpick.app.domain.place.PlaceCategory
import com.dulpick.app.domain.place.PlaceOwnership
import com.dulpick.app.domain.place.SavedPlace
import com.dulpick.app.domain.place.matches

enum class CoursePlaceLoad { LOADING, LOADED, FAILED }

// 코스에 담을 장소를 고르는 화면. 고른 순서가 곧 번호다 (iOS CoursePlacePickView 대응)
data class CoursePlacePickState(
    val places: List<SavedPlace> = emptyList(),
    val load: CoursePlaceLoad = CoursePlaceLoad.LOADING,
    val isCoupleConnected: Boolean = false,
    val selectedOwnership: PlaceOwnership = PlaceOwnership.TOGETHER,
    val selectedCategory: PlaceCategory? = null,
    // 순서가 곧 번호다. Set 을 쓰면 번호를 못 매긴다
    val selectedPlaceIds: List<String> = emptyList(),
    val isSavingCourse: Boolean = false,
    // 409 를 받은 뒤 최신 코스를 다시 읽었을 때 띄우는 알림
    val conflictMessage: String? = null,
) : UiState {
    val filteredPlaces: List<SavedPlace>
        get() = places
            .filter { selectedOwnership.matches(it.ownership) }
            .filter { selectedCategory == null || it.place.category == selectedCategory }

    val selectedCount: Int get() = selectedPlaceIds.size

    val ctaTitle: String
        get() = if (selectedCount == 0) "장소를 선택해주세요" else "${selectedCount}곳으로 코스짜기"

    val isCtaEnabled: Boolean get() = selectedCount >= 1 && !isSavingCourse

    val isEmpty: Boolean get() = load == CoursePlaceLoad.LOADED && filteredPlaces.isEmpty()

    val hasNoSavedPlace: Boolean get() = load == CoursePlaceLoad.LOADED && places.isEmpty()

    // 고른 번호. 안 골랐으면 null
    fun badgeNumber(id: String): Int? =
        selectedPlaceIds.indexOf(id).takeIf { it >= 0 }?.plus(1)
}

sealed interface CoursePlacePickIntent : UiIntent {
    data class Start(val dateCourseId: String) : CoursePlacePickIntent
    data object RetryClicked : CoursePlacePickIntent
    data class OwnershipSelected(val ownership: PlaceOwnership) : CoursePlacePickIntent
    data class CategorySelected(val category: PlaceCategory?) : CoursePlacePickIntent
    // 목록 행·지도 핀 둘 다 같은 토글이다
    data class PlaceToggled(val id: String) : CoursePlacePickIntent
    data object BuildClicked : CoursePlacePickIntent
    data object ConflictDismissed : CoursePlacePickIntent
    data object BackClicked : CoursePlacePickIntent
}

sealed interface CoursePlacePickSideEffect : UiSideEffect {
    // 확정 저장된 코스. 결과 화면이 받아 그린다
    data class BuildRequested(val dateCourseId: String) : CoursePlacePickSideEffect
    data object Dismissed : CoursePlacePickSideEffect
    data class ShowToast(val message: String) : CoursePlacePickSideEffect
    data object SessionExpired : CoursePlacePickSideEffect
}
