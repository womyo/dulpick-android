package com.dulpick.app.feature.course.component

import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dulpick.app.ui.theme.Colors
import com.dulpick.app.ui.theme.Typography
import kotlinx.coroutines.flow.distinctUntilChanged

// 날짜·시간 휠이 나눠 쓰는 치수 (iOS WheelMetrics 대응)
object WheelMetrics {
    // 264 = 위아래 여백 32 + 열 높이 200 + 32
    val areaHeight: Dp = 264.dp
    val columnHeight: Dp = 200.dp
    val columnWidth: Dp = 48.dp
    val columnSpacing: Dp = 72.dp
    val rowHeight: Dp = 24.dp

    // 행 사이 20 → 한 칸 44. 24×5 + 20×4 = 200 이라 다섯 행이 딱 보인다
    val rowSpacing: Dp = 20.dp

    // 한 칸(행 + 간격). 스냅과 가운데 계산이 이 값을 본다
    val slotHeight: Dp = rowHeight + rowSpacing

    // 첫·마지막 항목도 가운데로 올 수 있게 열 위아래에 두는 빈자리.
    // (열 높이 − 한 칸) ÷ 2 라야 offset 0 인 항목이 열 한가운데에 선다.
    // 한 칸보다 넉넉히 주면 스냅 자리와 선택 바가 어긋나 보정 스크롤이 끼어든다
    val columnPadding: Dp = (columnHeight - slotHeight) / 2

    val selectionBarHeight: Dp = 48.dp
    val selectionBarCornerRadius: Dp = 8.dp
    val selectionBarHorizontalInset: Dp = 20.dp
}

// 한 자리 수도 앞에 0 을 붙여 두 자리로 맞춘다. 열 폭이 고정이라 자릿수가 흔들리면 안 된다
fun twoDigits(value: Int): String = "%02d".format(value)

// 세 열을 가로지르는 선택 바. 열 뒤층에 깔리고 가운데 행에 세로 중심을 맞춘다
@Composable
fun WheelSelectionBar(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = WheelMetrics.selectionBarHorizontalInset)
            .height(WheelMetrics.selectionBarHeight)
            .clip(RoundedCornerShape(WheelMetrics.selectionBarCornerRadius))
            .background(Colors.bgSubtle),
    )
}

// 휠 한 열. 평평한 스크롤 목록이라 3D 로 말리지 않는다 (iOS WheelColumn 대응).
// 한 칸씩 멈추도록 스냅을 걸고, 가운데 칸에 온 항목을 고른 값으로 올린다
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> WheelColumn(
    items: List<T>,
    selected: T?,
    onSelect: (T) -> Unit,
    title: (T) -> String,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val currentItems by rememberUpdatedState(items)
    val currentOnSelect by rememberUpdatedState(onSelect)

    // 가운데 칸 = 첫 보이는 항목. 위아래 빈자리를 두 칸씩 뒀기 때문이다
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { (index, scrolling) ->
                if (scrolling) return@collect
                currentItems.getOrNull(index)?.let(currentOnSelect)
            }
    }

    // 밖에서 값이 바뀌면(달이 바뀌어 일이 내려간 경우 등) 그 자리로 다시 맞춘다.
    // 가리키는 칸이 실제로 다를 때만 움직인다 — 같은 칸인데 되감으면 사용자 손짓과 싸운다
    LaunchedEffect(selected, items) {
        val index = items.indexOf(selected)
        if (index < 0 || listState.isScrollInProgress) return@LaunchedEffect
        if (listState.firstVisibleItemIndex == index) return@LaunchedEffect
        listState.scrollToItem(index)
    }

    LazyColumn(
        state = listState,
        flingBehavior = rememberSnapFlingBehavior(listState),
        contentPadding = PaddingValues(vertical = WheelMetrics.columnPadding),
        modifier = modifier.size(width = WheelMetrics.columnWidth, height = WheelMetrics.columnHeight),
    ) {
        items(items.size) { index ->
            val item = items[index]
            val isSelected = item == selected
            Box(
                modifier = Modifier
                    .size(width = WheelMetrics.columnWidth, height = WheelMetrics.slotHeight),
                // 칸 가운데에 둔다. 위로 붙이면 선택 바 가운데에서 글자가 밀린다
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = title(item),
                    style = if (isSelected) Typography.body1SB else Typography.body1M,
                    color = if (isSelected) Colors.textPrimary else Colors.textTertiary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.height(WheelMetrics.rowHeight),
                )
            }
        }
    }
}
