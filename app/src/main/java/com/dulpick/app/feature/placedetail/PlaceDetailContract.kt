package com.dulpick.app.feature.placedetail

import com.dulpick.app.core.mvi.UiIntent
import com.dulpick.app.core.mvi.UiSideEffect
import com.dulpick.app.core.mvi.UiState
import com.dulpick.app.domain.explore.Content
import com.dulpick.app.domain.place.Place

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
    // 북마크 표시값. 헤더 북마크 아이콘·"저장한 사람 N" 이 이걸 본다
    val isBookmarked: Boolean = false,
    val bookmarkCount: Int = 0,
) : UiState {
    // 서버가 아는 장소여야 게시물을 부른다. 있고, 결과가 있거나 로딩·실패면 섹션을 보인다(0건이면 숨김)
    val showsRelatedSection: Boolean
        get() = serverPlaceId != null &&
            (contents.isNotEmpty() || contentsLoad != ContentsLoad.LOADED)

    // 카카오맵 앱/웹 어느 쪽이든 열 곳이 있는지
    val canOpenKakaoMap: Boolean
        get() = place?.kakaoPlaceId != null || kakaoPlaceUrl != null
}

sealed interface PlaceDetailIntent : UiIntent {
    data object OnAppear : PlaceDetailIntent
    data object AddressToggled : PlaceDetailIntent
    data object MapClicked : PlaceDetailIntent
    data object CloseClicked : PlaceDetailIntent
    data object MoreContentsClicked : PlaceDetailIntent
    data object RetryContentsClicked : PlaceDetailIntent
    data class ContentClicked(val id: String) : PlaceDetailIntent
    data object BookmarkClicked : PlaceDetailIntent
}

sealed interface PlaceDetailSideEffect : UiSideEffect {
    data object Close : PlaceDetailSideEffect
    // 카카오맵 앱/웹으로 열기 (앱 우선, 실패 시 웹)
    data class OpenKakaoMap(val appUri: String?, val webUrl: String?) : PlaceDetailSideEffect
    data class OpenContent(val id: String) : PlaceDetailSideEffect
    data object SessionExpired : PlaceDetailSideEffect
}
