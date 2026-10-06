package com.dulpick.app.ui.component

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.height
import kotlin.math.roundToInt

// 시트 안의 목록을 시트가 보이는 만큼 안에서만 스크롤한다.
//
// verticalScroll·LazyColumn 은 중첩 스크롤로 시트를 먼저 끌어올린다. 코스 화면의 시트는
// 손잡이로만 커지고 목록은 제자리에서 스크롤돼야 한다 (iOS SheetGestureKind.grabberOnly 대응).
// 그래서 목록의 제 손짓을 끄고(verticalScroll(enabled = false) · userScrollEnabled = false)
// 이 수정자로 직접 민다 — 중첩 스크롤을 타지 않아 시트가 반응하지 않는다.
//
// 목록이 손짓을 먼저 받으므로 머리말·손잡이를 끌 때만 시트가 움직인다
@Composable
fun Modifier.sheetContentScroll(
    state: ScrollableState,
    // 맨 위에서 아래로 끌 때 한 번 부른다. 더 스크롤할 곳이 없으니 시트를 접는 자리다
    onPullDownAtTop: () -> Unit = {},
): Modifier {
    val fling = ScrollableDefaults.flingBehavior()
    val currentPullDown by rememberUpdatedState(onPullDownAtTop)
    // 한 손짓에 한 번만 접는다
    var pulled by remember { mutableStateOf(false) }
    return draggable(
        state = rememberDraggableState { delta ->
            if (delta > 0 && !state.canScrollBackward) {
                if (!pulled) {
                    pulled = true
                    currentPullDown()
                }
            } else {
                state.dispatchRawDelta(-delta)
            }
        },
        orientation = Orientation.Vertical,
        onDragStarted = { pulled = false },
        // 손을 떼면 관성으로 더 간다. 목록 끝에 닿으면 거기서 멈춘다
        onDragStopped = { velocity -> state.scroll { with(fling) { performFling(-velocity) } } },
    )
}

// 시트가 보이는 만큼을 목록 뷰포트로 삼는다.
//
// 시트 내용의 높이는 펼침 기준으로 고정이다. 그대로 두면 접힌 상태에서 뷰포트가 화면 밖까지
// 뻗어 스크롤할 양이 0 이 된다 — 목록이 꿈쩍도 안 한다.
// 자기 위치를 재서 화면 바닥까지 남은 높이를 쓴다. 시트를 끌면 그만큼 늘어난다.
// rootHeightPx 는 화면을 덮는 바깥 칸의 높이다
@Composable
fun Modifier.sheetVisibleHeight(rootHeightPx: Int): Modifier {
    val density = LocalDensity.current
    // 첫 배치 전에는 높이를 모른다. 그때는 부모가 주는 높이를 그대로 쓴다
    var available by remember { mutableStateOf(-1) }
    return this
        .onGloballyPositioned { coordinates ->
            if (rootHeightPx > 0) {
                val top = coordinates.positionInRoot().y
                available = (rootHeightPx - top).roundToInt().coerceAtLeast(0)
            }
        }
        .then(if (available >= 0) Modifier.height(with(density) { available.toDp() }) else Modifier)
}
