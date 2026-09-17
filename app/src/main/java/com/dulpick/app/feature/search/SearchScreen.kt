package com.dulpick.app.feature.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dulpick.app.R
import com.dulpick.app.core.mvi.CollectSideEffect
import com.dulpick.app.feature.explore.component.ContentCard
import com.dulpick.app.feature.explore.component.ContentGridSkeleton
import com.dulpick.app.feature.search.component.PlaceRow
import com.dulpick.app.ui.component.AppTextField
import com.dulpick.app.ui.theme.Colors
import com.dulpick.app.ui.theme.Typography

private val HORIZONTAL_PADDING = 20.dp

@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onSessionExpired: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CollectSideEffect(viewModel.sideEffect) { effect ->
        when (effect) {
            // TODO: 게시물·장소 상세는 지도 단계에서
            is SearchSideEffect.ShowContentDetail -> Unit
            is SearchSideEffect.ShowPlaceDetail -> Unit
            SearchSideEffect.SessionExpired -> onSessionExpired()
        }
    }

    LaunchedEffect(Unit) { viewModel.onIntent(SearchIntent.OnAppear) }
    BackHandler { onBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Colors.bgDefault),
    ) {
        SearchTopBar(onBack = onBack)
        SearchField(
            query = state.query,
            onQueryChange = { viewModel.onIntent(SearchIntent.QueryChanged(it)) },
            onSubmit = { viewModel.onIntent(SearchIntent.SearchSubmitted) },
        )

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.showRecent -> RecentSection(
                    terms = state.recentSearches,
                    onTap = { viewModel.onIntent(SearchIntent.RecentTapped(it)) },
                    onDelete = { viewModel.onIntent(SearchIntent.RecentDeleted(it)) },
                    onClear = { viewModel.onIntent(SearchIntent.ClearRecent) },
                )
                else -> SearchResults(state = state, onIntent = viewModel::onIntent)
            }
        }
    }
}

@Composable
private fun SearchTopBar(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp),
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
        Text(text = "검색", style = Typography.body1SB, color = Colors.gray900)
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit, onSubmit: () -> Unit) {
    AppTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = "원하는 장소를 검색해보세요",
        imeAction = ImeAction.Search,
        onSubmit = onSubmit,
        trailingContent = {
            Image(
                painter = painterResource(R.drawable.search),
                contentDescription = null,
                colorFilter = ColorFilter.tint(Colors.gray400),
                modifier = Modifier.size(20.dp),
            )
        },
        modifier = Modifier.padding(horizontal = HORIZONTAL_PADDING, vertical = 20.dp),
    )
}

@Composable
private fun RecentSection(
    terms: List<String>,
    onTap: (String) -> Unit,
    onDelete: (String) -> Unit,
    onClear: () -> Unit,
) {
    if (terms.isEmpty()) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = HORIZONTAL_PADDING)
            // iOS: 검색창(아래 20) + 최근검색어(위 20) = 40 간격
            .padding(top = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "최근 검색어",
                style = Typography.title3SB,
                color = Colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "모두 지우기",
                style = Typography.body1SB,
                color = Colors.textTertiary,
                modifier = Modifier.clickable(onClick = onClear),
            )
        }
        // 3개씩 묶어 여러 줄로
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            terms.chunked(3).forEach { rowTerms ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowTerms.forEach { term ->
                        RecentChip(term = term, onTap = { onTap(term) }, onDelete = { onDelete(term) })
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentChip(term: String, onTap: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .border(1.dp, Colors.borderDefault, CircleShape)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = term,
            style = Typography.body1M,
            color = Colors.textTertiary,
            modifier = Modifier.clickable(onClick = onTap),
        )
        Image(
            painter = painterResource(R.drawable.x),
            contentDescription = "삭제",
            colorFilter = ColorFilter.tint(Colors.textTertiary),
            modifier = Modifier
                .size(16.dp)
                .clickable(onClick = onDelete),
        )
    }
}

// 결과 = 세그먼트 탭 + 본문. 첫 검색 중엔 탭(클릭 막음) + 게시글 스켈레톤을 보여준다 (iOS resultContent 대응)
@Composable
private fun SearchResults(state: SearchState, onIntent: (SearchIntent) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        // 결과가 있거나 첫 검색 로딩 중일 때만 탭을 노출한다
        if (state.hasSearchResult || state.isFirstSearch) {
            SegmentTabs(
                selected = state.selectedTab,
                // 첫 검색 로딩 중에는 탭 선택을 막는다
                enabled = state.hasSearchResult,
                onSelect = { onIntent(SearchIntent.TabSelected(it)) },
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.hasSearchResult && state.hasResult ->
                    if (state.selectedTab == SearchTab.POST) {
                        ResultGrid(state = state, onIntent = onIntent)
                    } else {
                        PlaceResultList(state = state, onIntent = onIntent)
                    }
                // 첫 결과 전(디바운스 대기·검색 중)엔 게시글 스켈레톤. 빈 상태가 먼저 깜빡이지 않게 한다
                state.isFirstSearch ->
                    ContentGridSkeleton(modifier = Modifier.padding(horizontal = HORIZONTAL_PADDING))
                else -> EmptyResult()
            }
        }
    }
}

@Composable
private fun SegmentTabs(selected: SearchTab, enabled: Boolean, onSelect: (SearchTab) -> Unit) {
    Row(
        modifier = Modifier.padding(horizontal = HORIZONTAL_PADDING),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SearchTab.entries.forEach { tab ->
            val isSelected = tab == selected
            // 밑줄이 글자 폭만큼만 되도록 컬럼 폭을 한 줄 글자 폭에 맞춘다.
            // Min 을 쓰면 한글은 글자마다 줄바꿈 가능해 한 글자 폭이 돼 세로로 쪼개진다 → Max 사용
            Column(
                modifier = Modifier
                    .width(IntrinsicSize.Max)
                    .clickable(enabled = enabled) { onSelect(tab) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = tab.title,
                    style = Typography.title3SB,
                    color = if (isSelected) Colors.textPrimary else Colors.textTertiary,
                    maxLines = 1,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(if (isSelected) Colors.textPrimary else Color.Transparent),
                )
            }
        }
    }
}

// 장소 결과 리스트 + 무한 스크롤
@Composable
private fun PlaceResultList(state: SearchState, onIntent: (SearchIntent) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = HORIZONTAL_PADDING, end = HORIZONTAL_PADDING, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(state.places, key = { index, place -> "${place.id}#$index" }) { index, place ->
            if (index == state.places.lastIndex && state.placesHasNext && !state.isLoadingMore) {
                LaunchedEffect(index, state.places.size) { onIntent(SearchIntent.ReachedEnd) }
            }
            PlaceRow(place = place, onClick = { onIntent(SearchIntent.PlaceClicked(place.id)) })
        }
        if (state.isLoadingMore) {
            item(key = "loadingMore") { CenteredLoading() }
        }
    }
}

// 게시글 결과 2열 그리드 + 무한 스크롤
@Composable
private fun ResultGrid(state: SearchState, onIntent: (SearchIntent) -> Unit) {
    val rows = state.contents.chunked(2)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = HORIZONTAL_PADDING, end = HORIZONTAL_PADDING, bottom = 20.dp),
    ) {
        itemsIndexed(rows, key = { _, row -> row.first().id }) { index, rowItems ->
            if (index == rows.lastIndex && state.contentsHasNext && !state.isLoadingMore) {
                LaunchedEffect(index, state.contents.size) { onIntent(SearchIntent.ReachedEnd) }
            }
            Row(
                modifier = Modifier.padding(top = if (index != 0) 32.dp else 0.dp),
                horizontalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                rowItems.forEach { content ->
                    ContentCard(
                        content = content,
                        onClick = { onIntent(SearchIntent.ContentClicked(content.id)) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
        if (state.isLoadingMore) {
            item(key = "loadingMore") { CenteredLoading() }
        }
    }
}

@Composable
private fun EmptyResult() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.placeempty),
            contentDescription = null,
            modifier = Modifier.size(140.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "검색 결과가 없어요", style = Typography.headline, color = Colors.textPrimary)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "다른 검색어를 입력해주세요",
            style = Typography.body2M,
            color = Colors.textTertiary,
            textAlign = TextAlign.Center,
        )
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
