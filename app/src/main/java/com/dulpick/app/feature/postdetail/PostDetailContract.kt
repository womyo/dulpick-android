package com.dulpick.app.feature.postdetail

import com.dulpick.app.core.mvi.UiIntent
import com.dulpick.app.core.mvi.UiSideEffect
import com.dulpick.app.core.mvi.UiState
import com.dulpick.app.domain.explore.PostDetailContent

// 게시글 상세. 지도 시트의 한 갈래로 붙는다 (iOS PostDetailFeature.State 대응)
data class PostDetailState(
    val detail: PostDetailContent? = null,
    // 화면 안에서만 뒤집는 북마크. 서버에 켜기·끄기 계약이 없다.
    // 시트를 닫았다 열면 서버 값으로 돌아간다
    val savedPlaceIds: Set<String> = emptySet(),
    // 본문을 전문으로 보일지. 시트 단계와는 무관하다
    val isExpanded: Boolean = false,
    val isLoading: Boolean = false,
    val loadFailed: Boolean = false,
) : UiState {
    // 원본에 제목이 없거나 빈 글자면 없음을 알린다. 자리를 비우지 않는다
    val displayTitle: String
        get() = detail?.title?.takeIf { it.isNotEmpty() } ?: "제목없음"
}

sealed interface PostDetailIntent : UiIntent {
    data class Start(val contentId: String) : PostDetailIntent
    data object RetryClicked : PostDetailIntent
    data object ExpandToggled : PostDetailIntent
    data object CloseClicked : PostDetailIntent
    data class PlaceClicked(val id: String) : PostDetailIntent
    data class PlaceBookmarkClicked(val id: String) : PostDetailIntent
}

sealed interface PostDetailSideEffect : UiSideEffect {
    // 상세를 받아왔다. 지도가 이 places 로 핀·카메라를 세운다
    data class DetailLoaded(val detail: PostDetailContent) : PostDetailSideEffect
    // X 탭. 지도가 시트를 원래대로 되돌린다
    data object Close : PostDetailSideEffect
    // 행 탭
    data class PlaceSelected(val id: String) : PostDetailSideEffect
    // 세션 만료. 루트까지 올라가 로그인으로 되돌린다
    data object SessionExpired : PostDetailSideEffect
}
