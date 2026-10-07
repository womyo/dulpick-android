package com.dulpick.app.feature.notice

import com.dulpick.app.core.mvi.UiIntent
import com.dulpick.app.core.mvi.UiSideEffect
import com.dulpick.app.core.mvi.UiState
import com.dulpick.app.domain.notice.Notice

// 공지 목록 (iOS NoticeListFeature 대응).
// 실패는 따로 보여주지 않는다 — 빈 목록으로 두고 "없어요" 를 띄운다 (iOS 와 같다)
data class NoticeListState(
    val notices: List<Notice> = emptyList(),
    // 다음에 받아올 페이지 번호
    val page: Int = 0,
    val hasNext: Boolean = true,
    val hasLoaded: Boolean = false,
    val isLoadingMore: Boolean = false,
    // 다음 장 받기가 실패했는지. 실패 뒤 마지막 줄이 다시 보일 때마다 자동 재요청이 반복되는 걸 막는다
    val loadMoreFailed: Boolean = false,
) : UiState {
    val isEmpty: Boolean get() = hasLoaded && notices.isEmpty()

    val canLoadMore: Boolean get() = hasLoaded && hasNext && !isLoadingMore && !loadMoreFailed

    companion object {
        const val PAGE_SIZE = 20
    }
}

sealed interface NoticeListIntent : UiIntent {
    data object OnAppear : NoticeListIntent
    data object ReachedEnd : NoticeListIntent
    data class NoticeClicked(val notice: Notice) : NoticeListIntent
    data object BackClicked : NoticeListIntent
}

sealed interface NoticeListSideEffect : UiSideEffect {
    // 상세 조회가 없어 값을 통째로 올린다
    data class OpenNotice(val notice: Notice) : NoticeListSideEffect
    data object Dismissed : NoticeListSideEffect
}
