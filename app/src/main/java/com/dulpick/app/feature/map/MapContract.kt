package com.dulpick.app.feature.map

import com.dulpick.app.core.mvi.UiIntent
import com.dulpick.app.core.mvi.UiSideEffect
import com.dulpick.app.core.mvi.UiState
import com.dulpick.app.domain.place.Place
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
    // null 이 아니면 검색 결과 모드. 저장목록 대신 검색 결과 리스트를 시트에 보이고 검색바를 바꾼다 (iOS searchResult 모드)
    val searchResult: SearchResult? = null,
    // 검색 결과 행에서 낙관적으로 저장/해제한 kakaoId 오버라이드(저장목록 반영 전까지)
    val bookmarkOverrides: Map<String, Boolean> = emptyMap(),
    // null 이 아니면 지도 위 저장목록/검색결과 시트 대신 이 장소의 상세 시트를 띄운다 (iOS mode=.content 대응)
    val detail: DetailTarget? = null,
) : UiState {
    // 지도 핀과 시트 목록이 함께 보는 하나의 배열. 소유자·카테고리 필터를 적용한다
    val filteredPlaces: List<SavedPlace>
        get() = places
            .filter { selectedOwnership.matches(it.ownership) }
            .filter { selectedCategory == null || it.place.category == selectedCategory }

    // 검색 결과 모드 여부
    val isSearching: Boolean get() = searchResult != null

    // 저장한 장소 자체가 없다(필터 때문에 빈 것과 문구가 다르다)
    val hasNoSavedPlace: Boolean get() = !isLoading && places.isEmpty()

    // 다 불러온 뒤 보일 게 없다(필터 결과 포함)
    val isEmpty: Boolean get() = !isLoading && filteredPlaces.isEmpty()

    // 검색 결과 장소의 저장 여부. 저장목록의 kakaoId 매칭 + 낙관적 오버라이드
    fun isBookmarked(place: Place): Boolean {
        val kakaoId = place.kakaoPlaceId ?: return false
        bookmarkOverrides[kakaoId]?.let { return it }
        return places.any { it.place.kakaoPlaceId == kakaoId }
    }
}

// 지도 검색 결과 모드 상태. searchQuery 는 상세 조회용 원본, displayQuery 는 검색바 표시용
data class SearchResult(
    val searchQuery: String,
    val displayQuery: String,
    val places: List<Place>,
)

// 지도 위에 띄우는 장소 상세 대상. 검색 결과는 카카오+검색어, 저장/핀 장소는 서버 placeId 로 조회한다
data class DetailTarget(
    val place: Place,
    val query: String,
    val serverPlaceId: Long?,
)

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
    data object CloseDetail : MapIntent
}

sealed interface MapSideEffect : UiSideEffect {
    data object SessionExpired : MapSideEffect
    // isError 면 에러 아이콘이 붙은 토스트로 띄운다 (iOS ToastState.error 대응)
    data class ShowToast(val message: String, val isError: Boolean) : MapSideEffect
}
