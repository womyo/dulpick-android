package com.dulpick.app.feature.placedetail

import com.dulpick.app.core.mvi.UiIntent
import com.dulpick.app.core.mvi.UiSideEffect
import com.dulpick.app.core.mvi.UiState
import com.dulpick.app.domain.explore.Content
import com.dulpick.app.domain.place.Place
import com.dulpick.app.ui.component.displayName


// 관련 게시물 로드 상태
enum class ContentsLoad { LOADING, LOADED, FAILED }

// iOS 처럼 place 를 들고 초기화하고, 상세 API 는 부가정보만 덧입힌다. 실패해도 place 로 화면을 그린다
data class PlaceDetailState(
    // 표시할 장소. 진입 시 넘겨받은 값으로 시작하고 상세 응답이 오면 갱신한다
    val place: Place? = null,
    // 진입 인자가 없어 아직 아무것도 못 그리는 초기 상태에만 로딩을 보인다
    val isLoading: Boolean = true,
    // 서버가 아는 장소 ID(상세 응답 placeId). 게시물·삭제에 쓴다
    val serverPlaceId: Long? = null,
    // 카카오맵 웹 주소(상세 응답)
    val kakaoPlaceUrl: String? = null,
    // 지번 주소 펼침 여부
    val isAddressExpanded: Boolean = false,
    val contents: List<Content> = emptyList(),
    val hasNextContents: Boolean = true,
    val contentsLoad: ContentsLoad = ContentsLoad.LOADING,
    // 북마크 표시값. 저장 버튼·"저장한 사람 N" 이 이걸 본다
    val isBookmarked: Boolean = false,
    val bookmarkCount: Int = 0,
    // 카카오 분류에서 고른 세부 업종
    val detailCategory: String? = null,
    // 「지도로 보기」 로 띄우는 지도 앱 고르기 시트
    val showsMapSheet: Boolean = false,
) : UiState {
    // 서버가 아는 장소여야 게시물을 부른다. 있고, 결과가 있거나 로딩·실패면 섹션을 보인다(0건이면 숨김)
    val showsRelatedSection: Boolean
        get() = serverPlaceId != null &&
            (contents.isNotEmpty() || contentsLoad != ContentsLoad.LOADED)

    // 카카오맵 앱/웹 어느 쪽이든 열 곳이 있는지
    val canOpenKakaoMap: Boolean
        get() = place?.kakaoPlaceId != null || kakaoPlaceUrl != null

    // 둘째 줄에 붙이는 세부 분류. 둘픽 분류와 같은 말이면 붙이지 않는다
    val detailCategoryText: String?
        get() {
            val text = detailCategory?.replace(',', '/')?.trim().orEmpty()
            if (text.isEmpty()) return null
            val category = place?.category ?: return text
            return text.takeUnless { spaceless(it) == spaceless(category.displayName()) }
        }

    private fun spaceless(text: String): String = text.filterNot { it.isWhitespace() }
}

sealed interface PlaceDetailIntent : UiIntent {
    // 진입 시 넘겨받은 장소로 초기화. serverPlaceId 가 있으면(저장/게시글 장소) 서버 조회, 없으면 카카오+검색어
    data class Start(val place: Place, val query: String, val serverPlaceId: Long?) : PlaceDetailIntent
    data object AddressToggled : PlaceDetailIntent
    data object MapClicked : PlaceDetailIntent
    data object MapSheetDismissed : PlaceDetailIntent
    data class MapAppSelected(val app: PlaceMapApp) : PlaceDetailIntent
    data object CloseClicked : PlaceDetailIntent
    data object MoreContentsClicked : PlaceDetailIntent
    data object RetryContentsClicked : PlaceDetailIntent
    data class ContentClicked(val id: String) : PlaceDetailIntent
    data object BookmarkClicked : PlaceDetailIntent
}

sealed interface PlaceDetailSideEffect : UiSideEffect {
    data object Close : PlaceDetailSideEffect
    // 지도 앱/웹으로 열기 (앱 우선, 없으면 웹)
    data class OpenMap(val appUri: String, val webUrl: String) : PlaceDetailSideEffect
    data class OpenContent(val id: String) : PlaceDetailSideEffect
    data object SessionExpired : PlaceDetailSideEffect
}
