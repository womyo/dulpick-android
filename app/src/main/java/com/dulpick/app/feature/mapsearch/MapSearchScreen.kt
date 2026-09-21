package com.dulpick.app.feature.mapsearch

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dulpick.app.R
import com.dulpick.app.core.mvi.CollectSideEffect
import com.dulpick.app.domain.place.Place
import com.dulpick.app.ui.component.ShimmerBox
import com.dulpick.app.ui.component.iconRes
import com.dulpick.app.ui.theme.Colors
import com.dulpick.app.ui.theme.Typography

private val HORIZONTAL = 20.dp
private const val SKELETON_ROWS = 3
private val SKELETON_ROW_HEIGHT = 64.dp
private val FIELD_HEIGHT = 48.dp

// 지도 전용 장소 검색 화면 (iOS PlaceSearchView 대응)
@Composable
fun MapSearchScreen(
    onBack: () -> Unit,
    onSearchConfirmed: (query: String, places: List<Place>) -> Unit,
    onPlaceSelected: (query: String, place: Place) -> Unit,
    onSessionExpired: () -> Unit,
    viewModel: MapSearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    BackHandler { onBack() }
    CollectSideEffect(viewModel.sideEffect) { effect ->
        when (effect) {
            is MapSearchSideEffect.SearchConfirmed -> onSearchConfirmed(effect.query, effect.places)
            is MapSearchSideEffect.PlaceSelected -> onPlaceSelected(effect.query, effect.place)
            MapSearchSideEffect.SessionExpired -> onSessionExpired()
        }
    }
    LaunchedEffect(Unit) { viewModel.onIntent(MapSearchIntent.OnAppear) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Colors.bgDefault)
            .statusBarsPadding(),
    ) {
        TopBar(
            query = state.query,
            onQueryChange = { viewModel.onIntent(MapSearchIntent.QueryChanged(it)) },
            onSubmit = { viewModel.onIntent(MapSearchIntent.SearchSubmitted) },
            onBack = onBack,
        )
        Content(state = state, onIntent = viewModel::onIntent)
    }
}

// 상단바: 뒤로가기 + outlined 검색 필드. 뒤로가기는 다른 화면과 통일(좌 12·아이콘 24·textPrimary·세로중앙)
@Composable
private fun TopBar(query: String, onQueryChange: (String) -> Unit, onSubmit: () -> Unit, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // 상단바가 상태바에 딱 붙지 않게 위 여백을 준다(iOS엔 없지만 안드로이드 통일감)
            .padding(top = 8.dp, end = HORIZONTAL, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.arrowleft),
                contentDescription = "뒤로",
                colorFilter = ColorFilter.tint(Colors.textPrimary),
                modifier = Modifier.size(24.dp),
            )
        }
        SearchField(
            query = query,
            onQueryChange = onQueryChange,
            onSubmit = onSubmit,
            modifier = Modifier.weight(1f),
        )
    }
}

// medium(높이 48·radius 12·수평 20) + outlined(bgDefault·borderDefault) + accessory (iOS AppTextField 대응)
@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .height(FIELD_HEIGHT)
            .clip(RoundedCornerShape(12.dp))
            .background(Colors.bgDefault)
            .border(1.dp, Colors.borderDefault, RoundedCornerShape(12.dp))
            .padding(horizontal = HORIZONTAL),
        singleLine = true,
        textStyle = Typography.body1M.copy(color = Colors.gray900),
        cursorBrush = SolidColor(Colors.gray900),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
        decorationBox = { innerTextField ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(text = "원하는 장소를 검색하세요", style = Typography.body1M, color = Colors.gray400)
                    }
                    innerTextField()
                }
                Spacer(modifier = Modifier.size(8.dp))
                // 비었으면 돋보기, 입력이 있으면 지우기(cancel)
                if (query.isEmpty()) {
                    Image(
                        painter = painterResource(R.drawable.search),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(Colors.textTertiary),
                        modifier = Modifier.size(24.dp),
                    )
                } else {
                    Image(
                        painter = painterResource(R.drawable.cancel),
                        contentDescription = "입력 지우기",
                        colorFilter = ColorFilter.tint(Colors.gray300),
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { onQueryChange("") },
                    )
                }
            }
        },
    )
}

@Composable
private fun Content(state: MapSearchState, onIntent: (MapSearchIntent) -> Unit) {
    when {
        state.showRecent -> RecentContent(state = state, onIntent = onIntent)
        state.isSearching || !state.hasSearched -> Skeleton()
        state.results.isEmpty() -> EmptyState(title = "검색 결과가 없어요", message = "다른 검색어를 입력해주세요")
        else -> ResultList(state = state, onIntent = onIntent)
    }
}

@Composable
private fun RecentContent(state: MapSearchState, onIntent: (MapSearchIntent) -> Unit) {
    if (state.recentSearches.isEmpty()) {
        EmptyState(title = "최근 검색한 기록이 없어요", message = "데이트 장소를 검색해보세요")
        return
    }
    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = HORIZONTAL, vertical = 17.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
                modifier = Modifier.clickable { onIntent(MapSearchIntent.ClearRecent) },
            )
        }
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(state.recentSearches, key = { it }) { term ->
                RecentRow(
                    term = term,
                    onTap = { onIntent(MapSearchIntent.RecentTapped(term)) },
                    onDelete = { onIntent(MapSearchIntent.RecentDeleted(term)) },
                )
            }
        }
    }
}

@Composable
private fun RecentRow(term: String, onTap: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap)
            .padding(horizontal = HORIZONTAL, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Colors.gray50),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.search),
                contentDescription = null,
                colorFilter = ColorFilter.tint(Colors.textTertiary),
                modifier = Modifier.size(16.dp),
            )
        }
        Text(
            text = term,
            style = Typography.body1M,
            color = Colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Image(
            painter = painterResource(R.drawable.x),
            contentDescription = "삭제",
            colorFilter = ColorFilter.tint(Colors.gray300),
            modifier = Modifier
                .size(20.dp)
                .clickable(onClick = onDelete),
        )
    }
}

// 검색 결과 행: 아이콘 + 이름 + 주소 + 구분선 (사진·우측슬롯 없음) (iOS PlaceListRow 검색 변형 대응)
@Composable
private fun ResultList(state: MapSearchState, onIntent: (MapSearchIntent) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        itemsIndexed(state.results, key = { _, place -> place.id }) { index, place ->
            if (index == (state.results.size - 3).coerceAtLeast(0) && state.hasNext && !state.isLoadingMore) {
                LaunchedEffect(index, state.results.size) { onIntent(MapSearchIntent.ReachedEnd) }
            }
            ResultRow(
                place = place,
                showsDivider = place.id != state.results.last().id,
                onClick = { onIntent(MapSearchIntent.PlaceClicked(place.id)) },
            )
        }
    }
}

@Composable
private fun ResultRow(place: Place, showsDivider: Boolean, onClick: () -> Unit) {
    Column(modifier = Modifier.clickable(onClick = onClick)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = HORIZONTAL, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(place.category.iconRes()),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = place.name,
                    style = Typography.body1M,
                    color = Colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = place.address,
                    style = Typography.caption1R,
                    color = Colors.textTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (showsDivider) {
            Box(
                modifier = Modifier
                    .padding(horizontal = HORIZONTAL)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Colors.borderWeak),
            )
        }
    }
}

@Composable
private fun Skeleton() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = HORIZONTAL)
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        repeat(SKELETON_ROWS) {
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(SKELETON_ROW_HEIGHT)
                    .clip(RoundedCornerShape(12.dp)),
                // iOS ShimmerBlock 기본 바탕색 gray50 (이미지 자리 gray300 과 다름)
                baseColor = Colors.gray50,
            )
        }
    }
}

// 빈/실패 공용. cancel 아이콘(40·borderDefault) + 제목 + 안내, 상단 정렬 (iOS EmptyStateView 대응).
// iOS 여백: 위 80(vertical40 + top40), 아래 40
@Composable
private fun EmptyState(title: String, message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.cancel),
            contentDescription = null,
            colorFilter = ColorFilter.tint(Colors.borderDefault),
            modifier = Modifier.size(40.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = title, style = Typography.title3SB, color = Colors.textPrimary)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = message, style = Typography.body1M, color = Colors.textTertiary)
    }
}
