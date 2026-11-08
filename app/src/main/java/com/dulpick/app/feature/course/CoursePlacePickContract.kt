package com.dulpick.app.feature.course

import com.dulpick.app.core.map.MapCamera
import com.dulpick.app.core.map.MapRoute
import com.dulpick.app.core.mvi.UiIntent
import com.dulpick.app.core.mvi.UiSideEffect
import com.dulpick.app.core.mvi.UiState
import com.dulpick.app.domain.place.Coordinate
import com.dulpick.app.domain.place.PlaceCategory
import com.dulpick.app.domain.place.PlaceOwnership
import com.dulpick.app.domain.place.SavedPlace
import com.dulpick.app.domain.place.matches

enum class CoursePlaceLoad { LOADING, LOADED, FAILED }

enum class CoursePlaceSearchLoad { IDLE, LOADING, LOADED, FAILED }

// 시트 목록 자리에 보일 것
enum class CoursePlaceListContent {
    SKELETON, LOAD_FAILED, NO_SAVED_PLACE, NO_MATCH, NO_SEARCH_RESULT, SEARCH_FAILED, LIST
}

// 「저장한 장소」 시트의 장소 검색. 검색어만 서버에 보내고 카테고리·저장자·코스에 있는 장소는 앱이 건다
data class CoursePlaceSearch(
    // 검색창이 열려 키보드가 올라와 있다. 닫히면 칩 줄로 돌아간다
    val isFieldOpen: Boolean = false,
    val query: String = "",
    val results: List<SavedPlace> = emptyList(),
    // 이 화면에서 검색으로 받은 장소 전부. 검색을 풀어도 남아 고른 장소를 찾는 데 쓴다
    val foundPlaces: List<SavedPlace> = emptyList(),
    val load: CoursePlaceSearchLoad = CoursePlaceSearchLoad.IDLE,
    val page: Int = 0,
    val hasNext: Boolean = false,
    val isLoadingMore: Boolean = false,
) {
    val trimmedQuery: String get() = query.trim()
}

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
    val search: CoursePlaceSearch = CoursePlaceSearch(),
    // 내 위치 점. 위치 흐름만 채운다
    val userLocation: Coordinate? = null,
    val camera: MapCamera = MapCamera.SEOUL_CITY_HALL,
    // 같은 자리를 다시 요청해도 지도가 움직이도록 요청마다 하나씩 올린다
    val cameraNonce: Long = 0,
    // 핀을 누르면 들어간다. 화면이 받아 목록을 그 행까지 올린다
    val rowScrollRequest: RowScrollRequest? = null,
    val isSavingCourse: Boolean = false,
    // 409 를 받은 뒤 최신 코스를 다시 읽었을 때 띄우는 알림
    val conflictMessage: String? = null,
) : UiState {
    // 검색 중이면 검색 결과, 아니면 받아 둔 목록에 지금 필터를 건다. 서버에는 검색어만 보낸다
    val filteredPlaces: List<SavedPlace>
        get() = applyFilters(if (isSearching) search.results else places)

    // 검색어가 있으면 검색 중이다. 목록·지도 핀이 검색 결과로 바뀐다
    val isSearching: Boolean get() = search.trimmedQuery.isNotEmpty()

    // 「검색」 칩은 목록이 뜬 뒤에만 열린다
    val isSearchAvailable: Boolean get() = load == CoursePlaceLoad.LOADED

    // 고른 장소를 찾는다. 받아 둔 목록에 없으면 이 화면에서 받은 검색 결과에서 찾는다
    fun savedPlace(id: String): SavedPlace? =
        places.firstOrNull { it.id == id } ?: search.foundPlaces.firstOrNull { it.id == id }

    // 고른 장소를 그 순서대로. 목록에서 필터로 사라진 장소도 남는다
    val selectedPlaces: List<SavedPlace>
        get() = selectedPlaceIds.mapNotNull(::savedPlace)

    val listContent: CoursePlaceListContent
        get() = when {
            load == CoursePlaceLoad.LOADING -> CoursePlaceListContent.SKELETON
            load == CoursePlaceLoad.FAILED -> CoursePlaceListContent.LOAD_FAILED
            !isSearching -> when {
                places.isEmpty() -> CoursePlaceListContent.NO_SAVED_PLACE
                filteredPlaces.isEmpty() -> CoursePlaceListContent.NO_MATCH
                else -> CoursePlaceListContent.LIST
            }
            else -> searchListContent
        }

    private val searchListContent: CoursePlaceListContent
        get() = when (search.load) {
            CoursePlaceSearchLoad.IDLE, CoursePlaceSearchLoad.LOADING -> CoursePlaceListContent.SKELETON
            CoursePlaceSearchLoad.FAILED -> CoursePlaceListContent.SEARCH_FAILED
            CoursePlaceSearchLoad.LOADED -> when {
                filteredPlaces.isNotEmpty() -> CoursePlaceListContent.LIST
                // 걸러져 비었지만 다음 페이지를 이어 부르는 중이다
                search.hasNext -> CoursePlaceListContent.SKELETON
                else -> CoursePlaceListContent.NO_SEARCH_RESULT
            }
        }

    // 보이는 행이 끝에 가까워졌고 받을 페이지가 남았는지
    fun shouldLoadMoreSearch(index: Int, visibleCount: Int): Boolean =
        isSearching && search.hasNext && !search.isLoadingMore &&
            index == (visibleCount - PREFETCH_ROWS).coerceAtLeast(0)

    // 저장자·카테고리 필터를 걸고 코스에 이미 있는 장소를 뺀다
    fun applyFilters(source: List<SavedPlace>): List<SavedPlace> {
        val taken = existingPlaces.map(ExistingCoursePlace::id).toSet()
        return source
            .filter { selectedOwnership.matches(it.ownership) }
            .filter { selectedCategory == null || it.place.category == selectedCategory }
            .filter { it.id !in taken }
    }

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

    // 고른 번호. 안 골랐으면 null.
    // 장소 추가에서는 기존 코스 뒤에 붙으므로 그 다음 번호부터 센다
    fun badgeNumber(id: String): Int? =
        selectedPlaceIds.indexOf(id).takeIf { it >= 0 }?.plus(existingPlaces.size + 1)

    private companion object {
        const val ROUTE_ID = "course-pick"
        const val PREFETCH_ROWS = 3
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
    data object SearchChipClicked : CoursePlacePickIntent
    data class SearchQueryChanged(val query: String) : CoursePlacePickIntent
    data object SearchReachedEnd : CoursePlacePickIntent
    // 키보드가 내려갔다. 검색 키·화면 탭·목록 스크롤 어느 것이든 같다
    data object SearchFieldDismissed : CoursePlacePickIntent
    data object SearchCancelClicked : CoursePlacePickIntent
    data object SearchChipClearClicked : CoursePlacePickIntent
    // 지도가 보이기 시작했다. 내 위치 흐름을 연다. 화면을 떠나면 뷰모델이 사라져 함께 끊긴다
    data object MapAppeared : CoursePlacePickIntent
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
