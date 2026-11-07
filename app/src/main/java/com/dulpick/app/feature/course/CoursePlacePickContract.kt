package com.dulpick.app.feature.course

import com.dulpick.app.core.map.MapCamera
import com.dulpick.app.core.map.MapRoute
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

// 목록을 이 행까지 올려 달라는 요청. 요청마다 번호가 하나씩 늘어 같은 행이어도 새 값이 된다
// (iOS RowScrollRequest 대응)
data class RowScrollRequest(val placeId: String, val sequence: Long)

// 코스에 담을 장소를 고르는 화면. 고른 순서가 곧 번호다 (iOS CoursePlacePickView 대응)
data class CoursePlacePickState(
    val mode: CoursePlacePickMode = CoursePlacePickMode.CREATE,
    // 더하기로 들어왔을 때 이미 코스에 담긴 장소. 목록에서 빼고 지도에는 기존 코스로 그린다
    val existingPlaces: List<ExistingCoursePlace> = emptyList(),
    val places: List<SavedPlace> = emptyList(),
    val load: CoursePlaceLoad = CoursePlaceLoad.LOADING,
    val isCoupleConnected: Boolean = false,
    val selectedOwnership: PlaceOwnership = PlaceOwnership.TOGETHER,
    val selectedCategory: PlaceCategory? = null,
    // 순서가 곧 번호다. Set 을 쓰면 번호를 못 매긴다
    val selectedPlaceIds: List<String> = emptyList(),
    val camera: MapCamera = MapCamera.SEOUL_CITY_HALL,
    // 같은 자리를 다시 요청해도 지도가 움직이도록 요청마다 하나씩 올린다
    val cameraNonce: Long = 0,
    // 핀을 누르면 들어간다. 화면이 받아 목록을 그 행까지 올린다
    val rowScrollRequest: RowScrollRequest? = null,
    val isSavingCourse: Boolean = false,
    // 409 를 받은 뒤 최신 코스를 다시 읽었을 때 띄우는 알림
    val conflictMessage: String? = null,
) : UiState {
    val filteredPlaces: List<SavedPlace>
        get() = places
            .filter { selectedOwnership.matches(it.ownership) }
            .filter { selectedCategory == null || it.place.category == selectedCategory }
            .filter { it.id !in existingPlaces.map(ExistingCoursePlace::id) }

    // 고른 장소를 그 순서대로. 목록에서 필터로 사라진 장소도 남는다
    val selectedPlaces: List<SavedPlace>
        get() = selectedPlaceIds.mapNotNull { id -> places.firstOrNull { it.id == id } }

    // 기존 코스 다음에 고른 순서대로 잇는 직선. 점이 둘 이상일 때만 긋는다 (iOS routes 대응)
    val routes: List<MapRoute>
        get() {
            val coordinates = existingPlaces.map(ExistingCoursePlace::coordinate) +
                selectedPlaces.map { it.place.coordinate }
            return if (coordinates.size >= 2) listOf(MapRoute(ROUTE_ID, coordinates)) else emptyList()
        }

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

    // 고른 번호. 안 골랐으면 null.
    // 장소 추가에서는 기존 코스 뒤에 붙으므로 그 다음 번호부터 센다
    fun badgeNumber(id: String): Int? =
        selectedPlaceIds.indexOf(id).takeIf { it >= 0 }?.plus(existingPlaces.size + 1)

    private companion object {
        const val ROUTE_ID = "course-pick"
    }
}

sealed interface CoursePlacePickIntent : UiIntent {
    // 더하기로 열면 코스 번호가 없다. 대신 이미 담긴 장소를 받아 목록에서 빼고 지도에 그린다
    data class Start(
        val dateCourseId: String?,
        val mode: CoursePlacePickMode,
        val existing: List<ExistingCoursePlace> = emptyList(),
    ) : CoursePlacePickIntent
    data object RetryClicked : CoursePlacePickIntent
    data class OwnershipSelected(val ownership: PlaceOwnership) : CoursePlacePickIntent
    data class CategorySelected(val category: PlaceCategory?) : CoursePlacePickIntent
    // 목록 행을 눌렀다. 고르고 그 장소로 카메라를 옮긴다
    data class RowClicked(val id: String) : CoursePlacePickIntent
    // 지도 핀을 눌렀다. 고르고 목록을 그 행까지 올린다. 기존 코스 핀은 아무 일도 없다
    data class PinTapped(val markerId: String) : CoursePlacePickIntent
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
