package com.dulpick.app.feature.map

import com.dulpick.app.core.mvi.UiIntent
import com.dulpick.app.core.mvi.UiSideEffect
import com.dulpick.app.core.mvi.UiState
import com.dulpick.app.domain.place.Coordinate
import com.dulpick.app.domain.place.Place
import com.dulpick.app.domain.place.PlaceCategory
import com.dulpick.app.domain.place.PlaceOwnership
import com.dulpick.app.domain.place.SavedPlace
import com.dulpick.app.domain.place.matches

data class MapState(
    val places: List<SavedPlace> = emptyList(),
    // 저장 장소를 아직 못 불러온 초기 상태
    val isLoading: Boolean = true,
    // 조회가 실패한 상태. 빈 목록("저장한 장소가 없어요")과 구분해 재시도를 보여준다
    val loadFailed: Boolean = false,
    // 커플 연동 시에만 소유자 필터를 노출한다
    val isCoupleConnected: Boolean = false,
    val selectedOwnership: PlaceOwnership = PlaceOwnership.TOGETHER,
    // null 이면 전체 카테고리
    val selectedCategory: PlaceCategory? = null,
    // null 이 아니면 별칭 편집 시트를 띄운다
    val aliasEdit: AliasEdit? = null,
    // null 이 아니면 검색 결과 모드. 저장목록 대신 검색 결과 리스트를 시트에 보이고 검색바를 바꾼다 (iOS searchResult 모드)
    val searchResult: SearchResult? = null,
    // 검색 결과 행에서 낙관적으로 저장/해제한 kakaoId 오버라이드(저장목록 반영 전까지)
    val bookmarkOverrides: Map<String, Boolean> = emptyMap(),
    // null 이 아니면 지도 위 저장목록/검색결과 시트 대신 이 장소의 상세 시트를 띄운다 (iOS mode=.content 대응)
    val detail: DetailTarget? = null,
    // 내 위치 버튼이 잡은 좌표. 지도가 이 좌표로 카메라를 옮긴다
    val currentLocation: CurrentLocation? = null,
    // 위치 권한 설정 안내 모달 표시 여부
    val showsLocationPermissionModal: Boolean = false,
    // null 이 아니면 게시글 상세 시트를 띄운다. 장소 상세와 함께 있을 수 있다 (iOS postDetail 대응)
    val postDetail: PostDetail? = null,
    // 두 상세가 같이 있을 때 위에 있는 쪽. 나중에 연 쪽이다 (iOS topDetail 대응)
    val topDetail: TopDetail? = null,
) : UiState {
    // 지도 핀과 시트 목록이 함께 보는 하나의 배열. 소유자·카테고리 필터를 적용한다
    val filteredPlaces: List<SavedPlace>
        get() = places
            .filter { selectedOwnership.matches(it.ownership) }
            .filter { selectedCategory == null || it.place.category == selectedCategory }

    // 검색 결과 모드 여부
    val isSearching: Boolean get() = searchResult != null

    // 위에 보일 시트가 게시글 상세인지. 둘 다 있으면 나중에 연 쪽이 이긴다
    val showsPostDetail: Boolean
        get() = postDetail != null && (detail == null || topDetail == TopDetail.POST)

    // 게시글 핀을 그릴지. 상세를 받아오기 전에는 원래 핀을 그대로 둔다
    val showsPostPins: Boolean
        get() = postDetail != null && postDetail.places.isNotEmpty()

    // 저장한 장소 자체가 없다(필터 때문에 빈 것과 문구가 다르다)
    val hasNoSavedPlace: Boolean get() = !isLoading && places.isEmpty()

    // 다 불러온 뒤 보일 게 없다(필터 결과 포함). 실패는 빈 목록이 아니라 실패로 보여준다
    val isEmpty: Boolean get() = !isLoading && !loadFailed && filteredPlaces.isEmpty()

    // 검색 결과 장소의 저장 여부. 저장목록의 kakaoId 매칭 + 낙관적 오버라이드
    fun isBookmarked(place: Place): Boolean {
        val kakaoId = place.kakaoPlaceId ?: return false
        bookmarkOverrides[kakaoId]?.let { return it }
        return places.any { it.place.kakaoPlaceId == kakaoId }
    }
}

// 내 위치로 옮길 카메라 목표. nonce 로 같은 좌표를 다시 눌러도 카메라가 움직인다
data class CurrentLocation(val coordinate: Coordinate, val nonce: Long)

// 지도 검색 결과 모드 상태. searchQuery 는 상세 조회용 원본, displayQuery 는 검색바 표시용
data class SearchResult(
    val searchQuery: String,
    val displayQuery: String,
    val places: List<Place>,
)

// 지도 위에 띄우는 장소 상세 대상. 검색 결과는 카카오+검색어, 저장/핀 장소는 서버 placeId 로 조회한다.
// contentMode 면 지도를 상세 전용으로 쓴다(iOS mode=.content 대응): 검색바·칩을 숨기고 그 장소 핀만 찍으며,
// 닫으면 온 곳(탐색 검색)으로 되돌린다
data class DetailTarget(
    val place: Place,
    val query: String,
    val serverPlaceId: Long?,
    val contentMode: Boolean = false,
)

// 지도 위에 띄우는 게시글 상세. places 는 상세를 받아온 뒤 채워져 핀·카메라에 쓰인다
data class PostDetail(
    val contentId: String,
    val places: List<Place> = emptyList(),
    // 다른 탭·화면에서 들어온 경우. 닫으면 온 곳으로 되돌린다 (iOS returnsAfterDetailClose 대응)
    val returnsOnClose: Boolean = false,
)

enum class TopDetail { PLACE, POST }

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
    data object RetryClicked : MapIntent
    data class OwnershipSelected(val ownership: PlaceOwnership) : MapIntent
    // null 이면 전체
    data class CategorySelected(val category: PlaceCategory?) : MapIntent
    data class DeleteClicked(val id: String) : MapIntent
    data class EditClicked(val id: String) : MapIntent
    data class AliasSaveClicked(val alias: String) : MapIntent
    data object AliasEditDismissed : MapIntent
    // 검색 화면에서 되돌아온 결과. 지도를 검색 결과 모드로 바꾼다(selectedIndex 있으면 상세도 연다)
    data class EnterSearchResult(
        val searchQuery: String,
        val displayQuery: String,
        val places: List<Place>,
        val selectedIndex: Int?,
    ) : MapIntent
    // 검색 결과 리스트 행 탭 → 그 장소 상세
    data class SearchRowClicked(val place: Place) : MapIntent
    // 검색 결과 행 북마크 토글(저장/해제)
    data class SearchBookmarkClicked(val place: Place) : MapIntent
    // 검색바 X(지우기) → 저장 모드로 복귀
    data object ClearSearch : MapIntent
    // 저장 장소 목록 행/핀 탭 → 서버 placeId 로 상세
    data class OpenSavedDetail(val place: SavedPlace) : MapIntent
    // 홈 등 다른 탭에서 넘어온 저장 장소 → 서버 placeId 로 상세(저장 모드, 검색 결과 아님)
    data class OpenPlaceDetail(val place: Place) : MapIntent
    // 탐색 검색에서 온 장소 → 상세 전용(content) 모드로 연다. 검색바 없이 그 장소 핀+상세만
    data class OpenContentDetail(val place: Place, val query: String) : MapIntent
    data object CloseDetail : MapIntent
    // 내 위치 버튼. 권한 상태에 따라 요청·조회·안내로 갈린다
    data object CurrentLocationClicked : MapIntent
    // 권한 요청 결과. granted 면 좌표를 조회하고, 영구 거부면 설정 안내를 띄운다
    data class LocationPermissionResult(val granted: Boolean, val permanentlyDenied: Boolean) : MapIntent
    data object LocationModalDismissed : MapIntent
    // 게시물 카드 탭(탐색·검색·홈·장소 상세) → 지도 위에 게시글 상세를 연다
    data class OpenPostDetail(val contentId: String, val returnsOnClose: Boolean = false) : MapIntent
    // 게시글 상세가 받아온 장소들. 지도 핀·카메라를 이걸로 세운다 (iOS contentPlacesApplied 대응).
    // 늦게 도착한 앞 게시글의 장소를 지금 게시글에 올리지 않도록 contentId 를 같이 받는다
    data class PostPlacesApplied(val contentId: String, val places: List<Place>) : MapIntent
    // 게시글 상세의 장소 행 탭 → 그 장소 상세를 위에 얹는다
    data class OpenPostPlaceDetail(val placeId: String) : MapIntent
    data object ClosePostDetail : MapIntent
}

sealed interface MapSideEffect : UiSideEffect {
    data object SessionExpired : MapSideEffect
    // isError 면 에러 아이콘이 붙은 토스트로 띄운다 (iOS ToastState.error 대응)
    data class ShowToast(val message: String, val isError: Boolean) : MapSideEffect
    // 다른 곳에서 들어온 게시글 상세를 닫았다. 화면이 온 곳으로 되돌린다
    data object PostDetailClosed : MapSideEffect
    // 위치 권한이 없다. 화면이 시스템 권한 요청을 띄운다
    data object RequestLocationPermission : MapSideEffect
}
