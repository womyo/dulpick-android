package com.dulpick.app.feature.onboarding.couple

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dulpick.app.core.mvi.CollectSideEffect
import com.dulpick.app.ui.theme.Colors

@Composable
fun CoupleScreen(
    onBack: () -> Unit,
    onFinished: () -> Unit,
    onSessionExpired: () -> Unit,
    viewModel: CoupleViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CollectSideEffect(viewModel.sideEffect) { effect ->
        when (effect) {
            CoupleSideEffect.Back -> onBack()
            CoupleSideEffect.Finished -> onFinished()
            CoupleSideEffect.SessionExpired -> onSessionExpired()
        }
    }

    LaunchedEffect(Unit) { viewModel.onIntent(CoupleIntent.OnAppear) }

    // 앱이 뒤에 있는 동안 상대가 연결했을 수 있다. 돌아온 순간 한 번 묻는다
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onIntent(CoupleIntent.SceneBecameActive)
    }

    // 완료 화면은 뒤로가기가 없다. 나머지는 ViewModel 이 step 을 보고 처리한다
    BackHandler(enabled = state.step != CoupleStep.COMPLETE) {
        viewModel.onIntent(CoupleIntent.BackClicked)
    }

    AnimatedContent(
        targetState = state.step,
        transitionSpec = {
            val forward = targetState.ordinal >= initialState.ordinal
            val width = if (forward) 1 else -1
            (slideInHorizontally(tween()) { width * it } + fadeIn()) togetherWith
                (slideOutHorizontally(tween()) { -width * it } + fadeOut())
        },
        modifier = Modifier
            .fillMaxSize()
            .background(Colors.bgDefault),
        label = "coupleStep",
    ) { step ->
        when (step) {
            CoupleStep.CONNECT -> CoupleConnectScreen(state = state, onIntent = viewModel::onIntent)
            CoupleStep.CODE_INPUT -> CoupleCodeInputScreen(state = state, onIntent = viewModel::onIntent)
            CoupleStep.COMPLETE -> CoupleCompleteScreen(state = state, onIntent = viewModel::onIntent)
        }
    }
}
