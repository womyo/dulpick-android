package com.dulpick.app.feature.appintro

import com.dulpick.app.core.mvi.UiIntent
import com.dulpick.app.core.mvi.UiSideEffect
import com.dulpick.app.core.mvi.UiState

data class AppIntroState(
    val pageIndex: Int = 0,
    val hasCompleted: Boolean = false,
) : UiState {
    val pages: List<AppIntroStep> = AppIntroStep.entries
    val pageCount: Int get() = pages.size
    val isFirstPage: Boolean get() = pageIndex <= 0
    val isLastPage: Boolean get() = pageIndex >= pageCount - 1
}

sealed interface AppIntroIntent : UiIntent {
    data object BackClicked : AppIntroIntent
    data object NextClicked : AppIntroIntent
    data class PageChanged(val index: Int) : AppIntroIntent
}

sealed interface AppIntroSideEffect : UiSideEffect {
    // 마지막 페이지에서 다음을 눌러 인트로를 끝냈다
    data object Completed : AppIntroSideEffect
}
