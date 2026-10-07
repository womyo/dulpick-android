package com.dulpick.app.feature.notice

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dulpick.app.R
import com.dulpick.app.core.mvi.CollectSideEffect
import com.dulpick.app.domain.notice.Notice
import com.dulpick.app.ui.component.ShimmerBox
import com.dulpick.app.ui.theme.Colors
import com.dulpick.app.ui.theme.Typography

// 줄 높이가 80 으로 고정이라 제목은 한 줄로 자른다
private val ROW_HEIGHT = 80.dp
private const val SKELETON_ROW_COUNT = 3

// 공지 목록 (iOS NoticeListView 대응)
@Composable
fun NoticeListScreen(
    onBack: () -> Unit,
    onOpenNotice: (Notice) -> Unit,
    viewModel: NoticeListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CollectSideEffect(viewModel.sideEffect) { effect ->
        when (effect) {
            NoticeListSideEffect.Dismissed -> onBack()
            is NoticeListSideEffect.OpenNotice -> onOpenNotice(effect.notice)
        }
    }
    LaunchedEffect(Unit) { viewModel.onIntent(NoticeListIntent.OnAppear) }
    BackHandler { viewModel.onIntent(NoticeListIntent.BackClicked) }

    Box(modifier = Modifier.fillMaxSize().background(Colors.bgDefault)) {
        // 빈 상태는 상단 띠를 뺀 자리가 아니라 화면 전체의 세로 가운데에 선다.
        // 띠 아래만 기준으로 잡으면 띠 높이의 절반만큼 아래로 치우쳐 보인다
        if (state.isEmpty) {
            EmptyState(modifier = Modifier.fillMaxSize())
        }
        Column(modifier = Modifier.fillMaxSize()) {
            NoticeTopBar(onBack = { viewModel.onIntent(NoticeListIntent.BackClicked) })
            when {
                // 첫 로드 전에는 빈 화면이 스치지 않게 목록과 같은 모양을 깔아 둔다
                !state.hasLoaded -> Skeleton()
                state.isEmpty -> Unit
                else -> NoticeList(state = state, onIntent = viewModel::onIntent)
            }
        }
    }
}

// 다른 push 화면과 같은 상단 바. 상태바 아래 56dp
@Composable
internal fun NoticeTopBar(onBack: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().statusBarsPadding().height(56.dp),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.arrowleft),
            contentDescription = "뒤로",
            colorFilter = ColorFilter.tint(Colors.textPrimary),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(horizontal = 12.dp)
                .size(24.dp)
                .clickable(onClick = onBack),
        )
        Text(text = "공지사항", style = Typography.body1SB, color = Colors.textPrimary)
    }
}

@Composable
private fun NoticeList(state: NoticeListState, onIntent: (NoticeListIntent) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        itemsIndexed(state.notices, key = { _, notice -> notice.id }) { index, notice ->
            // 마지막 줄이 보이면 다음 장을 받아 스크롤이 끊기지 않게 한다
            if (index == state.notices.lastIndex && state.canLoadMore) {
                LaunchedEffect(index, state.notices.size) { onIntent(NoticeListIntent.ReachedEnd) }
            }
            NoticeRow(notice = notice, onClick = { onIntent(NoticeListIntent.NoticeClicked(notice)) })
            RowDivider()
        }
        if (state.isLoadingMore) {
            item(key = "loadingMore") {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = Colors.primaryPink)
                }
            }
        }
    }
}

@Composable
private fun NoticeRow(notice: Notice, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(ROW_HEIGHT)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
            Text(
                text = notice.title,
                style = Typography.body1M,
                color = Colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = notice.createdAt.shortDateText(),
                style = Typography.caption1R,
                color = Colors.textTertiary,
            )
        }
        Image(
            painter = painterResource(R.drawable.arrowright),
            contentDescription = null,
            colorFilter = ColorFilter.tint(Colors.borderDefault),
            modifier = Modifier.size(24.dp),
        )
    }
}

// NoticeRow 와 같은 높이·여백을 쓴다. 로드 전후로 줄이 밀리지 않게 맞춘 값이다
@Composable
private fun Skeleton() {
    Column(modifier = Modifier.fillMaxWidth()) {
        repeat(SKELETON_ROW_COUNT) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ROW_HEIGHT)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ShimmerBox(modifier = Modifier.size(180.dp, 16.dp).clip(RoundedCornerShape(4.dp)))
                ShimmerBox(modifier = Modifier.size(60.dp, 13.dp).clip(RoundedCornerShape(4.dp)))
            }
            RowDivider()
        }
    }
}

@Composable
private fun RowDivider() {
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Colors.bgSubtle))
}

// 그림과 글자를 한 묶음으로 두고 그 묶음을 세로 가운데에 세운다
@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(painter = painterResource(R.drawable.datescheduleempty), contentDescription = null)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "아직 올라온 공지사항이 없어요",
            style = Typography.title3SB,
            color = Colors.textPrimary,
        )
    }
}
