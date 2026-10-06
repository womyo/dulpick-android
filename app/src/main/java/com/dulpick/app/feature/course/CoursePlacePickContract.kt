package com.dulpick.app.feature.course

import com.dulpick.app.core.mvi.UiIntent
import com.dulpick.app.core.mvi.UiSideEffect
import com.dulpick.app.core.mvi.UiState
import com.dulpick.app.domain.place.PlaceCategory
import com.dulpick.app.domain.place.PlaceOwnership
import com.dulpick.app.domain.place.SavedPlace
import com.dulpick.app.domain.place.matches

enum class CoursePlaceLoad { LOADING, LOADED, FAILED }

// 이 화면을 무엇에 쓰는지. 코스를 처음 짤 때와 수정 중 장소를 더할 때가 다르다 (iOS Mode 대응)
enum class CoursePlacePickMode { CREATE, ADD }

// 코스에 담을 장소를 고르는 화면. 고른 순서가 곧 번호다 (iOS CoursePlacePickView 대응)
data class CoursePlacePickState(
    val mode: CoursePlacePickMode = CoursePlacePickMode.CREATE,
    // 더하기로 들어왔을 때 이미 코스에 담긴 장소. 목록에서 뺀다
    val excludedPlaceIds: List<String> = emptyList(),
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
            .filter { it.id !in excludedPlaceIds }

    val selectedCount: Int get() = selectedPlaceIds.size

    val ctaTitle: String
        get() = when {
            mode == CoursePlacePickMode.ADD -> "추가"
            selectedCount == 0 -> "장소를 선택해주세요"
            else -> "${selectedCount}곳으로 코스짜기"
        }

    val isCtaEnabled: Boolean get() = selectedCount >= 1 && !isSavingCourse

    val isEmpty: Boolean get() = load == CoursePlaceLoad.LOADED && filteredPlaces.isEmpty()

    val hasNoSavedPlace: Boolean get() = load == CoursePlaceLoad.LOADED && places.isEmpty()

    // 고른 번호. 안 골랐으면 null
    fun badgeNumber(id: String): Int? =
        selectedPlaceIds.indexOf(id).takeIf { it >= 0 }?.plus(1)
}

sealed interface CoursePlacePickIntent : UiIntent {
    // 더하기로 열면 코스 번호가 없다. 대신 이미 담긴 장소를 받아 목록에서 뺀다
    data class Start(
        val dateCourseId: String?,
        val mode: CoursePlacePickMode,
        val excluding: List<String> = emptyList(),
    ) : CoursePlacePickIntent
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
    // 더하기로 열렸을 때. 코스를 저장하지 않고 고른 장소만 돌려준다
    data class PlacesPicked(val places: List<SavedPlace>) : CoursePlacePickSideEffect
    data object Dismissed : CoursePlacePickSideEffect
    data class ShowToast(val message: String) : CoursePlacePickSideEffect
    data object SessionExpired : CoursePlacePickSideEffect
}
