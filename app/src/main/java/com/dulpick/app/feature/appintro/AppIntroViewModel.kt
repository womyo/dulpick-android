package com.dulpick.app.feature.appintro

import com.dulpick.app.core.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AppIntroViewModel @Inject constructor() :
    MviViewModel<AppIntroState, AppIntroIntent, AppIntroSideEffect>(AppIntroState()) {

    override fun onIntent(intent: AppIntroIntent) {
        when (intent) {
            AppIntroIntent.BackClicked -> moveBack()
            AppIntroIntent.NextClicked -> moveNext()
            is AppIntroIntent.PageChanged -> changePage(intent.index)
        }
    }

    private fun moveBack() {
        if (currentState.isFirstPage) return
        setState { copy(pageIndex = pageIndex - 1) }
    }

    private fun moveNext() {
        val state = currentState
        if (state.hasCompleted) return
        if (state.isLastPage) {
            setState { copy(hasCompleted = true) }
            postSideEffect(AppIntroSideEffect.Completed)
        } else {
            setState { copy(pageIndex = pageIndex + 1) }
        }
    }

    private fun changePage(index: Int) {
        val clamped = index.coerceIn(0, currentState.pageCount - 1)
        if (clamped == currentState.pageIndex) return
        setState { copy(pageIndex = clamped) }
    }
}
