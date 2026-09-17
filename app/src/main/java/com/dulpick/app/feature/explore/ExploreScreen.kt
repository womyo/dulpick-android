package com.dulpick.app.feature.explore

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dulpick.app.R
import com.dulpick.app.core.mvi.CollectSideEffect
import com.dulpick.app.feature.explore.component.ContentCard
import com.dulpick.app.feature.explore.component.FilterChip
import com.dulpick.app.ui.theme.Colors
import com.dulpick.app.ui.theme.Typography

private val HORIZONTAL_PADDING = 20.dp

@Composable
fun ExploreScreen(
    onSessionExpired: () -> Unit,
    viewModel: ExploreViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CollectSideEffect(viewModel.sideEffect) { effect ->
        when (effect) {
            // TODO: 검색 화면·게시물 상세는 다음 단계
            ExploreSideEffect.SearchRequested -> Unit
            is ExploreSideEffect.ShowContentDetail -> Unit
            ExploreSideEffect.SessionExpired -> onSessionExpired()
        }
    }

    LaunchedEffect(Unit) { viewModel.onIntent(ExploreIntent.OnAppear) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Colors.bgDefault),
    ) {
        ExploreTopBar(onSearch = { viewModel.onIntent(ExploreIntent.SearchClicked) })
        ExploreList(state = state, onIntent = viewModel::onIntent)
    }
}

@Composable
private fun ExploreTopBar(onSearch: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = HORIZONTAL_PADDING),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(text = "탐색", style = Typography.title2B, color = Colors.gray900)
        Image(
            painter = painterResource(R.drawable.search),
            contentDescription = "검색",
            colorFilter = ColorFilter.tint(Colors.textSecondary),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(24.dp)
                .clickable(onClick = onSearch),
        )
    }
}

// iOS 간격 그대로: 상단 6 → 필터칩 → 24 → 제목 → 12 → 그리드(행 32 · 열 11)
@Composable
private fun ExploreList(state: ExploreState, onIntent: (ExploreIntent) -> Unit) {
    val rows = state.contents.chunked(2)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = HORIZONTAL_PADDING,
            end = HORIZONTAL_PADDING,
            top = 6.dp,
            bottom = 20.dp,
        ),
    ) {
        item(key = "filters") {
            FilterChipsRow(
                filters = state.filters,
                selectedFilter = state.selectedFilter,
                onSelect = { onIntent(ExploreIntent.FilterTapped(it)) },
            )
        }
        item(key = "title") {
            Text(
                text = state.sectionTitle,
                style = Typography.title2B,
                color = Colors.textPrimary,
                modifier = Modifier.padding(top = 24.dp, bottom = 12.dp),
            )
        }

        if (state.isInitialLoading) {
            item(key = "initialLoading") { CenteredLoading() }
        } else {
            itemsIndexed(rows, key = { _, row -> row.first().id }) { index, rowItems ->
                if (index == rows.lastIndex && state.hasNext && !state.isLoadingContents) {
                    // 마지막 행이 보이면 다음 페이지를 미리 받는다
                    LaunchedEffect(index, state.contents.size) { onIntent(ExploreIntent.ReachedEnd) }
                }
                ContentRow(
                    rowItems = rowItems,
                    topGap = index != 0,
                    onClick = { onIntent(ExploreIntent.ContentClicked(it)) },
                )
            }
            if (state.isLoadingContents) {
                item(key = "loadingMore") { CenteredLoading() }
            }
        }
    }
}

@Composable
private fun ContentRow(
    rowItems: List<com.dulpick.app.domain.explore.Content>,
    topGap: Boolean,
    onClick: (String) -> Unit,
) {
    Row(
        modifier = Modifier.padding(top = if (topGap) 32.dp else 0.dp),
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        rowItems.forEach { content ->
            ContentCard(
                content = content,
                onClick = { onClick(content.id) },
                modifier = Modifier.weight(1f),
            )
        }
        // 홀수로 남은 칸은 왼쪽 정렬 유지를 위해 빈 칸으로 채운다
        if (rowItems.size == 1) {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun FilterChipsRow(filters: List<String>, selectedFilter: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        filters.forEach { filter ->
            FilterChip(
                title = filter,
                isSelected = filter == selectedFilter,
                onClick = { onSelect(filter) },
            )
        }
    }
}

@Composable
private fun CenteredLoading() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = Colors.primaryPink)
    }
}
