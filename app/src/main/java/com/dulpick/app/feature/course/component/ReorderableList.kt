package com.dulpick.app.feature.course.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.dulpick.app.R
import com.dulpick.app.ui.theme.Colors
import com.dulpick.app.ui.theme.Typography
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

// 시안 c04 에서 잰 값. 드롭 위치를 고정 피치로 계산하므로 행 높이와 간격은 바뀌면 안 된다
private val ROW_HEIGHT = 81.dp
private val ROW_SPACING = 20.dp
private val CORNER_RADIUS = 16.dp

// 보이는 손잡이 아이콘 크기와, 그 위에 얹는 터치 범위
private val HANDLE_ICON_SIZE = 24.dp
private val HANDLE_HIT_SIZE = 40.dp

// 평상시 그림자와 끄는 동안의 그림자. 안드로이드는 고도로만 낸다
private val REST_ELEVATION = 2.dp
private val LIFT_ELEVATION = 8.dp

// 끄는 카드는 커지지 않는다. 들렸다는 느낌은 그림자가 내고 가로 중심만 10 왼쪽으로 간다
private val LIFT_OFFSET_X = (-10).dp

// 점선 중심 x. 카드 왼쪽 테두리 기준이고 손잡이 중심과 다르다
private val CONNECTOR_CENTER_X = 16.dp

// 점선이 카드에 닿지 않게 위아래로 한 주기씩 띄운다
private val CONNECTOR_INSET = DottedLine.DOT_SPACING

// 경로 끝에 딱 걸리는 점은 그려지지 않아 마지막 점 뒤로 반 주기를 더 준다
private val CONNECTOR_LENGTH = ROW_SPACING - CONNECTOR_INSET * 2 + DottedLine.DOT_SPACING / 2

// 행 하나가 차지하는 세로 거리. 드롭 위치 계산의 단위다
private val PITCH = ROW_HEIGHT + ROW_SPACING

private const val MOVE_ANIMATION_MS = 200

// 카드에 찍히는 글자 묶음
private data class RowText(val title: String, val subtitle: String, val category: String?)

// 끄는 동안의 상태. 배열은 건드리지 않고 이동량과 목적지만 들고 있는다
private class ReorderState(private val pitchPx: Float) {
    var draggingKey by mutableStateOf<String?>(null)
        private set
    var dropIndex by mutableStateOf(0)
        private set
    var translation by mutableStateOf(0f)
        private set
    private var sourceIndex by mutableStateOf<Int?>(null)

    val source: Int? get() = sourceIndex

    fun start(key: String, index: Int) {
        draggingKey = key
        sourceIndex = index
        dropIndex = index
        translation = 0f
    }

    fun drag(delta: Float, count: Int) {
        val from = sourceIndex ?: return
        // 목록 밖으로 카드가 빠져나가지 않게 위아래 끝에 묶는다
        val lower = -pitchPx * from
        val upper = pitchPx * max(count - 1 - from, 0)
        translation = min(max(translation + delta, lower), upper)
        // 이동량을 행 피치로 나눠 반올림한 칸 수가 곧 목적지다
        val movedRows = (translation / pitchPx).roundToInt()
        dropIndex = min(max(from + movedRows, 0), max(count - 1, 0))
    }

    fun reset() {
        draggingKey = null
        sourceIndex = null
        translation = 0f
        dropIndex = 0
    }

    // 끌지 않는 행이 비켜주는 양. 지나온 행만 한 칸씩 반대로 민다
    fun restOffset(index: Int): Float {
        val from = sourceIndex ?: return 0f
        return when {
            index == from -> 0f
            from < dropIndex && index > from && index <= dropIndex -> -pitchPx
            dropIndex < from && index >= dropIndex && index < from -> pitchPx
            else -> 0f
        }
    }
}

// 왼쪽 손잡이로만 끌어서 순서를 바꾸는 목록 (iOS ReorderableList 대응).
//
// 배열은 이 부품이 고치지 않는다. 끄는 동안의 이동량만 들고 있다가 손을 뗄 때
// onMove(from, to) 로 확정된 자리만 올린다. 순서를 실제로 바꾸는 것은 부르는 쪽이다
@Composable
@Suppress("LongParameterList")
fun <T> ReorderableList(
    items: List<T>,
    itemKey: (T) -> String,
    title: (T) -> String,
    subtitle: (T) -> String,
    onMove: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier,
    category: ((T) -> String)? = null,
    trailing: @Composable (T) -> Unit = {},
) {
    val density = LocalDensity.current
    val state = remember(density) { ReorderState(with(density) { PITCH.toPx() }) }
    val keys = items.map(itemKey)

    // 끄는 중에 밖에서 그 항목이 지워지면 손을 떼는 신호가 오지 않는다.
    // 그대로 두면 없는 열쇠를 가리킨 채 나머지 행이 한 칸 밀린 모양으로 굳는다
    LaunchedEffect(keys) {
        if (state.draggingKey != null && state.draggingKey !in keys) state.reset()
    }

    Box(modifier = modifier.fillMaxWidth()) {
        Connectors(count = items.size)
        Column(verticalArrangement = Arrangement.spacedBy(ROW_SPACING)) {
            items.forEachIndexed { index, item ->
                val rowKey = itemKey(item)
                // 행의 움직임 상태를 자리가 아니라 항목에 묶는다. 순서가 바뀌면 상태도 함께 간다
                key(rowKey) {
                    val isDragging = state.draggingKey == rowKey
                    // 비켜주는 행만 움직임을 입힌다. 끄는 카드는 손가락을 그대로 따라간다.
                    // 손을 뗀 뒤에는 배열이 이미 바뀌었으니 제자리로 즉시 둔다 —
                    // 움직임을 주면 카드가 엉뚱한 칸에서 미끄러져 들어온다
                    val settled by animateFloatAsState(
                        targetValue = state.restOffset(index),
                        animationSpec = if (state.draggingKey == null) snap() else tween(MOVE_ANIMATION_MS),
                        label = "reorderOffset",
                    )
                    Box(
                        modifier = Modifier
                            .zIndex(if (isDragging) 1f else 0f)
                            .offset {
                                IntOffset(
                                    x = if (isDragging) LIFT_OFFSET_X.roundToPx() else 0,
                                    y = if (isDragging) {
                                        state.translation.roundToInt()
                                    } else {
                                        settled.roundToInt()
                                    },
                                )
                            },
                    ) {
                        PlaceCard(
                            text = RowText(title(item), subtitle(item), category?.invoke(item)),
                            isDragging = isDragging,
                            handleModifier = Modifier.dragHandle(state, items, rowKey, index, itemKey, onMove),
                            trailing = { trailing(item) },
                        )
                    }
                }
            }
        }
    }
}

// 손잡이에만 다는 손짓. 손을 뗄 때 확정된 자리만 부르는 쪽에 올린다
@Suppress("LongParameterList")
private fun <T> Modifier.dragHandle(
    state: ReorderState,
    items: List<T>,
    rowKey: String,
    index: Int,
    itemKey: (T) -> String,
    onMove: (from: Int, to: Int) -> Unit,
): Modifier = pointerInput(rowKey, items.size) {
    detectVerticalDragGestures(
        onDragStart = { state.start(rowKey, index) },
        onDragEnd = {
            // 끄는 도중 밖에서 배열이 바뀌었을 수 있어 시작할 때 잡은 자리를 믿지 않는다
            val source = items.indexOfFirst { itemKey(it) == rowKey }
            val drop = state.dropIndex
            if (source == state.source && source != drop && drop in items.indices) {
                onMove(source, drop)
            }
            state.reset()
        },
        onDragCancel = { state.reset() },
    ) { _, delta -> state.drag(delta, items.size) }
}

// 카드 사이 빈 구간을 잇는 세로 점선. 카드 뒤 고정 배치라 끄는 카드를 따라가지 않는다.
// 마지막 카드 아래에는 그리지 않는다
@Composable
private fun Connectors(count: Int) {
    repeat(max(count - 1, 0)) { index ->
        DottedVerticalLine(
            color = Colors.borderDefault,
            modifier = Modifier
                .height(CONNECTOR_LENGTH)
                .offset {
                    IntOffset(
                        x = (CONNECTOR_CENTER_X - DottedLine.WIDTH / 2).roundToPx(),
                        y = (ROW_HEIGHT + CONNECTOR_INSET + PITCH * index).roundToPx(),
                    )
                },
        )
    }
}

@Composable
private fun PlaceCard(
    text: RowText,
    isDragging: Boolean,
    handleModifier: Modifier,
    trailing: @Composable () -> Unit,
) {
    // 손잡이와 오른쪽 칸은 카드 한가운데가 아니라 장소명 첫 줄의 중심에 선다.
    // 글자 블록은 아래로만 자라므로 이 선은 움직이지 않는다
    val titleLineHeight = with(LocalDensity.current) { Typography.headline.lineHeight.toDp() }

    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(ROW_HEIGHT)
            .shadow(
                elevation = if (isDragging) LIFT_ELEVATION else REST_ELEVATION,
                shape = RoundedCornerShape(CORNER_RADIUS),
            )
            .clip(RoundedCornerShape(CORNER_RADIUS))
            .background(Colors.commonWhite)
            .border(1.dp, Colors.borderDefault, RoundedCornerShape(CORNER_RADIUS))
            .padding(vertical = 16.dp, horizontal = 12.dp),
    ) {
        Handle(lineHeight = titleLineHeight, modifier = handleModifier)

        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = text.title,
                    style = Typography.headline,
                    color = Colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (!text.category.isNullOrEmpty()) {
                    Text(
                        text = text.category,
                        style = Typography.body2M,
                        color = Colors.textTertiary,
                        maxLines = 1,
                    )
                }
            }
            Text(
                text = text.subtitle,
                style = Typography.caption1R,
                color = Colors.textTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(modifier = Modifier.size(8.dp))
        Box(modifier = Modifier.height(titleLineHeight), contentAlignment = Alignment.Center) {
            trailing()
        }
    }
}

// 카드 본체가 아니라 이 손잡이에만 손짓을 단다.
//
// 자리는 보이는 아이콘 24 그대로 두고, 터치 범위 40 은 그 위에 얹는다.
// 40 을 자리에 넣으면 아이콘 중심이 왼쪽 여백 + 아이콘 절반에서 밀린다
@Composable
private fun Handle(lineHeight: Dp, modifier: Modifier) {
    Box(modifier = Modifier.height(lineHeight), contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.size(HANDLE_ICON_SIZE)) {
            Image(
                painter = painterResource(R.drawable.move),
                contentDescription = "순서 바꾸기",
                colorFilter = ColorFilter.tint(Colors.brandPrimary),
                modifier = Modifier.size(HANDLE_ICON_SIZE),
            )
            // 아이콘이 터치 범위의 위쪽 가운데에 오게 얹는다. 자리는 아이콘 크기로 고정이라 밀리지 않는다
            Box(
                modifier = Modifier
                    .requiredSize(HANDLE_HIT_SIZE)
                    .align(Alignment.TopCenter)
                    .then(modifier),
            )
        }
    }
}
