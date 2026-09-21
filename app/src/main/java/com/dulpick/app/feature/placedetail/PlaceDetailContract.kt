package com.dulpick.app.feature.placedetail

import com.dulpick.app.core.mvi.UiIntent
import com.dulpick.app.core.mvi.UiSideEffect
import com.dulpick.app.core.mvi.UiState
import com.dulpick.app.domain.explore.Content
import com.dulpick.app.domain.place.PlaceDetail

// 관련 게시물 로드 상태
enum class ContentsLoad { LOADING, LOADED, FAILED }

data class PlaceDetailState(
    val isLoading: Boolean = true,
    val isFailed: Boolean = false,
    val detail: PlaceDetail? = null,
    // 지번 주소 펼침 여부
    val isAddressExpanded: Boolean = false,
    val contents: List<Content> = emptyList(),
    val hasNextContents: Boolean = true,
    val contentsLoad: ContentsLoad = ContentsLoad.LOADING,
) : UiState {
    val title: String get() = detail?.place?.name.orEmpty()

    // 서버 ID 가 있어야 게시물을 부른다. 있고, 결과가 있거나 로딩·실패면 섹션을 보인다(0건이면 숨김)
    val showsRelatedSection: Boolean
        get() = detail?.place?.id?.toLongOrNull() != null &&
            (contents.isNotEmpty() || contentsLoad != ContentsLoad.LOADED)
}

sealed interface PlaceDetailIntent : UiIntent {
    data object OnAppear : PlaceDetailIntent
    data object AddressToggled : PlaceDetailIntent
    data object MapClicked : PlaceDetailIntent
    data object CloseClicked : PlaceDetailIntent
    data object MoreContentsClicked : PlaceDetailIntent
    data object RetryContentsClicked : PlaceDetailIntent
    data class ContentClicked(val id: String) : PlaceDetailIntent
}

sealed interface PlaceDetailSideEffect : UiSideEffect {
    data object Close : PlaceDetailSideEffect
    // 카카오맵 앱/웹으로 열기 (앱 우선, 실패 시 웹)
    data class OpenKakaoMap(val appUri: String?, val webUrl: String?) : PlaceDetailSideEffect
    data class OpenContent(val id: String) : PlaceDetailSideEffect
    data object SessionExpired : PlaceDetailSideEffect
}
