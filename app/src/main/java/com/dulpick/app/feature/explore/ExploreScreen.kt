package com.dulpick.app.feature.explore

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.dulpick.app.R
import com.dulpick.app.domain.explore.Content
import com.dulpick.app.feature.explore.component.ContentCard
import com.dulpick.app.feature.explore.component.FilterChip
import com.dulpick.app.ui.theme.Colors
import com.dulpick.app.ui.theme.Typography

private val HORIZONTAL_PADDING = 20.dp

// 탐색 메인 화면. 이 단계는 UI 만 — 검색 화면·상세·API 는 이후 단계
@Composable
fun ExploreScreen() {
    var selectedFilter by remember { mutableStateOf(FILTERS.first()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Colors.bgDefault),
    ) {
        ExploreTopBar(onSearch = {})

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 6.dp, bottom = 20.dp),
        ) {
            FilterChipsRow(selectedFilter = selectedFilter, onSelect = { selectedFilter = it })

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (selectedFilter == FILTERS.first()) "인기 게시물" else selectedFilter,
                style = Typography.title2B,
                color = Colors.textPrimary,
                modifier = Modifier.padding(horizontal = HORIZONTAL_PADDING),
            )

            Spacer(modifier = Modifier.height(12.dp))

            ContentGrid()
        }
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

@Composable
private fun FilterChipsRow(selectedFilter: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = HORIZONTAL_PADDING)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        FILTERS.forEach { filter ->
            FilterChip(
                title = filter,
                isSelected = filter == selectedFilter,
                onClick = { onSelect(filter) },
            )
        }
    }
}

// 2열 그리드. 행 사이 32, 열 사이 11 (iOS LazyVGrid 대응)
@Composable
private fun ContentGrid() {
    Column(
        modifier = Modifier.padding(horizontal = HORIZONTAL_PADDING),
        verticalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        SAMPLE_CONTENTS.chunked(2).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                rowItems.forEach { content ->
                    ContentCard(
                        content = content,
                        onClick = {},
                        modifier = Modifier.weight(1f),
                    )
                }
                // 홀수로 남은 칸은 왼쪽 정렬 유지를 위해 빈 칸으로 채운다
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// TODO: 실제 필터·게시물은 API 단계에서. 지금은 UI 확인용 샘플
private val FILTERS = listOf("인기", "#성수", "#연남", "#데이트", "#카페", "#분위기맛집")

private val SAMPLE_CONTENTS = List(8) { index ->
    Content(
        id = index.toLong(),
        title = "성수동 데이트 코스 추천 ${index + 1} — 카페부터 저녁까지",
        placeCount = 3 + index % 4,
        thumbnailUrls = emptyList(),
    )
}
