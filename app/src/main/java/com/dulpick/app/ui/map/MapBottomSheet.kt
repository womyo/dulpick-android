package com.dulpick.app.ui.map

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.dulpick.app.ui.component.SheetHandle
import com.dulpick.app.ui.theme.Colors
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

// 시트가 멈추는 자리. 최소는 3단계 시트에만 있다
enum class MapSheetDetent { MINIMUM, COLLAPSED, EXPANDED }

private val SHEET_CORNER_RADIUS = 32.dp

private val SETTLE_SPRING = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow,
)

// 이만큼 세게 던지면 가까운 자리 대신 던진 쪽의 다음 자리로 간다 (px/s)
private const val FLING_VELOCITY = 800f

// 시트가 멈출 자리의 보이는 높이
data class MapSheetHeights(
    val collapsed: Dp,
    val expanded: Dp,
    // null 이면 최소 없는 2단계 시트다
    val minimum: Dp? = null,
) {
    // 낮은 것부터
    val detents: List<MapSheetDetent>
        get() = if (minimum == null) {
            listOf(MapSheetDetent.COLLAPSED, MapSheetDetent.EXPANDED)
        } else {
            listOf(MapSheetDetent.MINIMUM, MapSheetDetent.COLLAPSED, MapSheetDetent.EXPANDED)
        }

    // 최소가 접힘을 넘으면 단계 순서가 뒤집힌다
    fun height(detent: MapSheetDetent): Dp = when (detent) {
        MapSheetDetent.MINIMUM -> minimum?.coerceAtMost(collapsed) ?: collapsed
        MapSheetDetent.COLLAPSED -> collapsed
        MapSheetDetent.EXPANDED -> expanded
    }
}

@Stable
class MapBottomSheetState(initial: MapSheetDetent) {
    var detent by mutableStateOf(initial)
        internal set

    // 시트 윗면의 y. 담는 칸 위 끝에서 잰 픽셀이다. NaN 이면 아직 자리를 못 잡았다
    internal var top by mutableFloatStateOf(Float.NaN)

    // 붙는 애니메이션 전용. 끄는 동안은 top 을 직접 쓴다
    private val settle = Animatable(0f)

    // 끈 만큼 옮기고 실제로 쓴 양을 돌려준다. 끝 자리 밖으로는 안 나간다
    internal fun drag(delta: Float, highest: Float, lowest: Float): Float {
        if (top.isNaN()) return 0f
        val moved = (top + delta).coerceIn(highest, lowest)
        val used = moved - top
        top = moved
        return used
    }

    internal suspend fun settleTo(target: MapSheetDetent, topFor: (MapSheetDetent) -> Float) {
        detent = target
        if (top.isNaN()) return
        settle.snapTo(top)
        settle.animateTo(topFor(target), SETTLE_SPRING) { top = value }
    }

    // 화면이 단계를 직접 바꾼다. 지도 손짓·필터 메뉴가 쓴다
    suspend fun moveTo(target: MapSheetDetent, topFor: (MapSheetDetent) -> Float) = settleTo(target, topFor)
}

@Composable
fun rememberMapBottomSheetState(
    initial: MapSheetDetent = MapSheetDetent.COLLAPSED,
): MapBottomSheetState = remember { MapBottomSheetState(initial) }

// 화면이 단계를 직접 옮기는 길
@Stable
class MapSheetMover internal constructor(
    private val state: MapBottomSheetState,
    private val detents: List<MapSheetDetent>,
    private val move: (MapSheetDetent) -> Unit,
) {
    // 지도를 손으로 끌었다. 보이는 중간 시트만 최소로 내린다. 최소·펼침은 그대로 둔다
    fun lowerForMapGesture() {
        if (state.detent == MapSheetDetent.COLLAPSED) move(MapSheetDetent.MINIMUM)
    }

    // 드롭다운이 필터 아래로 펼쳐져 최소에서는 탭바에 가린다. 그때만 중간으로 올린다
    fun raiseForMenu() {
        if (state.detent == MapSheetDetent.MINIMUM) move(MapSheetDetent.COLLAPSED)
    }

    // 목록 맨 위에서 아래로 끌었다. 한 단계 내린다
    fun stepDown() {
        move(detents.getOrNull(detents.indexOf(state.detent) - 1) ?: state.detent)
    }
}

@Composable
fun rememberSheetMover(
    state: MapBottomSheetState,
    heights: MapSheetHeights,
    containerHeight: Dp,
): MapSheetMover {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    return remember(state, heights, containerHeight, density) {
        val containerPx = with(density) { containerHeight.toPx() }
        val topFor = { detent: MapSheetDetent -> containerPx - with(density) { heights.height(detent).toPx() } }
        MapSheetMover(state, heights.detents) { target ->
            if (target in heights.detents && target != state.detent) {
                scope.launch { state.moveTo(target, topFor) }
            }
        }
    }
}

// 지도 위에 띄우는 시트. 손잡이를 끌거나 목록을 끝까지 끌면 단계 사이를 오간다
@Composable
fun MapBottomSheet(
    state: MapBottomSheetState,
    heights: MapSheetHeights,
    containerHeight: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val containerPx = with(density) { containerHeight.toPx() }
    val topFor = remember(heights, containerPx, density) {
        { detent: MapSheetDetent -> containerPx - with(density) { heights.height(detent).toPx() } }
    }
    val lowest = topFor(heights.detents.first())
    val highest = topFor(MapSheetDetent.EXPANDED)

    // 높이가 바뀌면(탭바 유무·필터 줄 측정) 지금 단계의 자리로 다시 맞춘다
    LaunchedEffect(heights, containerPx) {
        if (state.detent !in heights.detents) state.detent = MapSheetDetent.COLLAPSED
        if (state.top.isNaN()) state.top = topFor(state.detent) else state.settleTo(state.detent, topFor)
    }

    val connection = remember(heights, containerPx) {
        sheetNestedScroll(
            state = state,
            highest = highest,
            lowest = lowest,
            isAtDetent = { heights.detents.any { abs(state.top - topFor(it)) < 0.5f } },
            onSettle = { velocity ->
                scope.launch { state.settleTo(heights.settleTarget(state.top, velocity, topFor), topFor) }
            },
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset { IntOffset(0, (if (state.top.isNaN()) containerPx else state.top).roundToInt()) }
                .fillMaxWidth()
                .height(heights.expanded)
                .clip(RoundedCornerShape(topStart = SHEET_CORNER_RADIUS, topEnd = SHEET_CORNER_RADIUS))
                .background(Colors.commonWhite)
                .nestedScroll(connection),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .sheetDrag(state, heights, topFor, highest, lowest),
            ) {
                SheetHandle()
            }
            content()
        }
    }
}

@Composable
@Suppress("LongParameterList")
private fun Modifier.sheetDrag(
    state: MapBottomSheetState,
    heights: MapSheetHeights,
    topFor: (MapSheetDetent) -> Float,
    highest: Float,
    lowest: Float,
): Modifier = draggable(
    state = rememberDraggableState { delta -> state.drag(delta, highest, lowest) },
    orientation = Orientation.Vertical,
    onDragStopped = { velocity ->
        state.settleTo(heights.settleTarget(state.top, velocity, topFor), topFor)
    },
)

// 목록이 끝에 닿으면 시트가 이어받는다.
// 펼침이 아니면 올리는 손짓을 시트가 먼저 먹고, 목록 맨 위에서 내리면 시트가 내려간다
@Suppress("LongParameterList")
private fun sheetNestedScroll(
    state: MapBottomSheetState,
    highest: Float,
    lowest: Float,
    isAtDetent: () -> Boolean,
    onSettle: (Float) -> Unit,
): NestedScrollConnection = object : NestedScrollConnection {

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (source != NestedScrollSource.Drag || available.y >= 0) return Offset.Zero
        // 위로 끈다. 펼치기 전까지는 시트가 먼저 오른다
        return Offset(0f, state.drag(available.y, highest, lowest))
    }

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        if (source != NestedScrollSource.Drag || available.y <= 0) return Offset.Zero
        // 목록이 더 못 내려간다. 남은 만큼 시트를 내린다
        return Offset(0f, state.drag(available.y, highest, lowest))
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        // 시트가 단계에 앉아 있으면 관성은 목록 몫이다. 뺏으면 목록이 손을 떼는 순간 멈춘다
        if (isAtDetent()) return Velocity.Zero
        onSettle(available.y)
        return available
    }
}

// 세게 던지면 그 방향의 다음 자리로, 아니면 가장 가까운 자리로. 거리가 같으면 낮은 쪽이다
private fun MapSheetHeights.settleTarget(
    currentTop: Float,
    velocity: Float,
    topFor: (MapSheetDetent) -> Float,
): MapSheetDetent {
    if (currentTop.isNaN()) return MapSheetDetent.COLLAPSED
    val ordered = detents
    val nearest = ordered.minBy { abs(currentTop - topFor(it)) }
    if (abs(velocity) < FLING_VELOCITY) return nearest
    // 아래로 던지면 보이는 높이가 줄어든다. 낮은 단계가 목록 앞쪽이다
    val next = ordered.indexOf(nearest) + if (velocity > 0) -1 else 1
    return ordered.getOrElse(next) { nearest }
}
