package com.dulpick.app.feature.appintro

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dulpick.app.R
import com.dulpick.app.core.mvi.CollectSideEffect
import com.dulpick.app.ui.component.AppButton
import com.dulpick.app.ui.component.AppButtonSize
import com.dulpick.app.ui.component.AppButtonVariant
import com.dulpick.app.ui.component.CtaContainer
import com.dulpick.app.ui.theme.Colors
import com.dulpick.app.ui.theme.Typography
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppIntroScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AppIntroViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { state.pageCount })

    CollectSideEffect(viewModel.sideEffect) { effect ->
        when (effect) {
            AppIntroSideEffect.Completed -> onFinished()
        }
    }

    // 스와이프로 페이지가 바뀌면 상태에 반영
    LaunchedEffect(pagerState) {
        snapshotPage(pagerState) { viewModel.onIntent(AppIntroIntent.PageChanged(it)) }
    }
    // 버튼으로 pageIndex 가 바뀌면 페이저를 따라 움직인다
    LaunchedEffect(state.pageIndex) {
        if (pagerState.currentPage != state.pageIndex) {
            pagerState.animateScrollToPage(state.pageIndex)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Colors.bgDefault)
            .systemBarsPadding(),
    ) {
        BackBar(visible = !state.isFirstPage, onBack = { viewModel.onIntent(AppIntroIntent.BackClicked) })

        Spacer(modifier = Modifier.weight(1f))

        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            HorizontalPager(state = pagerState) { page ->
                IntroPage(step = state.pages[page])
            }
            PageIndicator(pageCount = state.pageCount, activeIndex = state.pageIndex)
        }

        Spacer(modifier = Modifier.weight(1f))

        CtaContainer {
            AppButton(
                text = "다음",
                onClick = { viewModel.onIntent(AppIntroIntent.NextClicked) },
                variant = AppButtonVariant.DARK,
                size = AppButtonSize.XL,
                fullWidth = true,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private suspend fun snapshotPage(
    pagerState: androidx.compose.foundation.pager.PagerState,
    onPage: (Int) -> Unit,
) {
    androidx.compose.runtime.snapshotFlow { pagerState.currentPage }
        .distinctUntilChanged()
        .collect(onPage)
}

@Composable
private fun BackBar(visible: Boolean, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().height(56.dp)) {
        if (visible) {
            Image(
                painter = painterResource(R.drawable.arrowleft),
                contentDescription = "이전",
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(horizontal = 12.dp)
                    .size(24.dp)
                    .clickable(onClick = onBack),
            )
        }
    }
}

@Composable
private fun IntroPage(step: AppIntroStep) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(40.dp),
    ) {
        androidx.compose.material3.Text(
            text = step.title,
            style = Typography.title1B,
            color = Colors.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Image(
            painter = painterResource(step.image),
            contentDescription = null,
            modifier = Modifier.size(width = 340.dp, height = 300.dp),
        )
    }
}

@Composable
private fun PageIndicator(pageCount: Int, activeIndex: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().height(32.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            val isActive = index == activeIndex
            val width by animateDpAsState(if (isActive) 17.dp else 8.dp, label = "dotWidth")
            val color: Color = if (isActive) Colors.textPrimary else Colors.borderDefault
            Box(
                modifier = Modifier
                    .width(width)
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}
