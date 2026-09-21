package com.dulpick.app.feature.mapsearch

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dulpick.app.R
import com.dulpick.app.core.mvi.CollectSideEffect
import com.dulpick.app.domain.place.Place
import com.dulpick.app.feature.search.component.PlaceRow
import com.dulpick.app.ui.component.AppTextField
import com.dulpick.app.ui.theme.Colors
import com.dulpick.app.ui.theme.Typography

private val HORIZONTAL_PADDING = 20.dp

// 지도 전용 장소 검색 화면. 결과는 콜백으로 지도에 반영한다 (iOS PlaceSearchView 대응)
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
            .background(Colors.bgDefault),
    ) {
        SearchTopBar(onBack = onBack)
        SearchField(
            query = state.query,
            onQueryChange = { viewModel.onIntent(MapSearchIntent.QueryChanged(it)) },
            onSubmit = { viewModel.onIntent(MapSearchIntent.SearchSubmitted) },
        )
        when {
            state.showRecent -> RecentSection(
                terms = state.recentSearches,
                onTap = { viewModel.onIntent(MapSearchIntent.RecentTapped(it)) },
                onDelete = { viewModel.onIntent(MapSearchIntent.RecentDeleted(it)) },
                onClear = { viewModel.onIntent(MapSearchIntent.ClearRecent) },
            )
            state.isEmptyResult -> EmptyResult()
            else -> PlaceResultList(state = state, onIntent = viewModel::onIntent)
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
        Text(text = "장소 검색", style = Typography.body1SB, color = Colors.gray900)
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

@Composable
private fun PlaceResultList(state: MapSearchState, onIntent: (MapSearchIntent) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = HORIZONTAL_PADDING,
            end = HORIZONTAL_PADDING,
            top = 8.dp,
            bottom = 20.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(state.results, key = { _, place -> place.id }) { index, place ->
            if (index == state.results.lastIndex && state.hasNext && !state.isLoadingMore) {
                LaunchedEffect(index, state.results.size) { onIntent(MapSearchIntent.ReachedEnd) }
            }
            PlaceRow(place = place, onClick = { onIntent(MapSearchIntent.PlaceClicked(place.id)) })
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
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = Colors.primaryPink)
    }
}
