package com.dulpick.app.feature.course

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dulpick.app.R
import com.dulpick.app.core.map.KakaoMapView
import com.dulpick.app.core.map.MapCamera
import com.dulpick.app.core.map.MapPin
import com.dulpick.app.core.map.MapRoute
import com.dulpick.app.core.map.MapZoom
import com.dulpick.app.core.mvi.CollectSideEffect
import com.dulpick.app.domain.place.Coordinate
import com.dulpick.app.feature.course.component.CourseTimeline
import com.dulpick.app.feature.map.numberedPin
import com.dulpick.app.feature.map.placePin
import com.dulpick.app.feature.map.rememberMapSheetState
import com.dulpick.app.ui.component.AppButton
import com.dulpick.app.ui.component.AppButtonSize
import com.dulpick.app.ui.component.AppButtonVariant
import com.dulpick.app.ui.component.AppToast
import com.dulpick.app.ui.component.CtaContainer
import com.dulpick.app.ui.component.ShimmerBox
import com.dulpick.app.ui.theme.Colors
import com.dulpick.app.ui.theme.Typography

private const val SHEET_PEEK_FRACTION = 0.42f
private const val SHEET_EXPANDED_FRACTION = 0.74f
private val SHEET_CORNER_RADIUS = 32.dp
private const val SKELETON_ROW_COUNT = 3
private val SKELETON_ROW_HEIGHT = 64.dp

// 탭바가 없는 화면이라 펼침 높이를 그만큼 더해 다른 시트와 맞춘다
private val TAB_BAR_HEIGHT = 80.dp

// 마지막 행이 CTA 버튼 위로 올라오게 비우는 높이
private val CTA_COVER_PADDING = 96.dp

// 확정된 코스를 보는 화면. 지도 + 타임라인 + 상대에게 알리기 (iOS CourseResultView 대응)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseResultScreen(
    dateCourseId: String,
    origin: CourseResultOrigin,
    onBack: () -> Unit,
    onSessionExpired: () -> Unit,
    onEdit: (dateCourseId: String) -> Unit,
    viewModel: CourseResultViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var toastMessage by remember { mutableStateOf<String?>(null) }
    val screenHeight = LocalConfiguration.current.screenHeightDp
    val sheetHeight = (screenHeight * SHEET_EXPANDED_FRACTION).dp + TAB_BAR_HEIGHT +
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    CollectSideEffect(viewModel.sideEffect) { effect ->
        when (effect) {
            CourseResultSideEffect.Dismissed -> onBack()
            CourseResultSideEffect.SessionExpired -> onSessionExpired()
            is CourseResultSideEffect.ShowToast -> toastMessage = effect.message
            is CourseResultSideEffect.EditRequested -> onEdit(effect.dateCourseId)
        }
    }
    LaunchedEffect(dateCourseId) {
        viewModel.onIntent(CourseResultIntent.Start(dateCourseId, origin))
    }
    BackHandler { viewModel.onIntent(CourseResultIntent.BackClicked) }

    Box(modifier = Modifier.fillMaxSize().background(Colors.bgDefault)) {
        BottomSheetScaffold(
            scaffoldState = rememberMapSheetState(),
            sheetPeekHeight = (screenHeight * SHEET_PEEK_FRACTION).dp,
            sheetContainerColor = Colors.commonWhite,
            containerColor = Color.Transparent,
            sheetTonalElevation = 0.dp,
            sheetShape = RoundedCornerShape(topStart = SHEET_CORNER_RADIUS, topEnd = SHEET_CORNER_RADIUS),
            sheetContent = { SheetBody(state = state, height = sheetHeight, onIntent = viewModel::onIntent) },
        ) {
            CourseMap(
                state = state,
                sheetPeekHeight = (screenHeight * SHEET_PEEK_FRACTION).dp,
                onBack = { viewModel.onIntent(CourseResultIntent.BackClicked) },
            )
        }

        if (state.showsNotifyButton) {
            Column(modifier = Modifier.align(Alignment.BottomCenter)) {
                CtaContainer {
                    AppButton(
                        text = state.notifyTitle,
                        onClick = { viewModel.onIntent(CourseResultIntent.NotifyClicked) },
                        variant = AppButtonVariant.PRIMARY,
                        size = AppButtonSize.XL,
                        enabled = !state.isNotifyingPartner,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        AppToast(message = toastMessage, onDismiss = { toastMessage = null }, bottomInset = 100)
    }
}

// 카테고리 핀 위에 번호 물방울을 얹고, 순서대로 선을 잇는다 (iOS markers·routes 대응)
@Composable
private fun CourseMap(state: CourseResultState, sheetPeekHeight: Dp, onBack: () -> Unit) {
    val context = LocalContext.current
    val pins = state.stops.flatMapIndexed { index, stop ->
        listOf<MapPin>(
            placePin(context, stop.id, stop.place.coordinate, stop.place.category),
            // 같은 장소 id 를 쓰면 지도가 하나를 버린다
            numberedPin(context, "numbered:${stop.id}", stop.place.coordinate, index + 1),
        )
    }
    val routes = if (state.stops.size >= 2) {
        listOf(MapRoute("course", state.stops.map { it.place.coordinate }))
    } else {
        emptyList()
    }

    // 시트 위에 보이는 띠. 지도는 화면 전체를 덮고 그 아래를 시트가 가린다
    val density = LocalDensity.current
    val peekHeightPx = with(density) { sheetPeekHeight.toPx() }
    var mapSize by remember { mutableStateOf(IntSize.Zero) }
    val collapsedSheetTop = (mapSize.height - peekHeightPx).coerceAtLeast(0f)
    // 줌 계산은 dp 로 넘긴다. 카메라 오프셋만 픽셀이다
    val camera = courseCamera(
        coordinates = state.stops.map { it.place.coordinate },
        viewWidth = with(density) { mapSize.width.toDp().value },
        visibleHeight = with(density) { collapsedSheetTop.toDp().value },
    )

    Box(modifier = Modifier.fillMaxSize()) {
        KakaoMapView(
            modifier = Modifier.fillMaxSize().onSizeChanged { mapSize = it },
            pins = pins,
            routes = routes,
            camera = camera,
            collapsedSheetTopPx = collapsedSheetTop,
        )
        BackButton(onClick = onBack)
    }
}

// 첫 장소를 초점에 두고, 모든 장소가 시트 위에 들어오는 줌을 고른다.
// 멀리 떨어진 장소가 섞이면 그만큼 줌아웃한다 (iOS mapSizeChanged 대응).
// 가로·세로는 dp 다. 크기를 재기 전에는 여러 장소용 기본 줌이다
@Composable
private fun courseCamera(coordinates: List<Coordinate>, viewWidth: Float, visibleHeight: Float): MapCamera {
    val anchor = coordinates.firstOrNull() ?: return MapCamera.SEOUL_CITY_HALL
    return remember(coordinates, viewWidth, visibleHeight) {
        MapCamera(
            center = anchor,
            zoomLevel = MapZoom.fit(
                coordinates = coordinates,
                anchor = anchor,
                viewWidth = viewWidth,
                visibleHeight = visibleHeight,
                maximum = MapCamera.MULTI_PLACE_ZOOM,
                focusRatio = MapZoom.MAP_FOCUS_RATIO,
            ),
        )
    }
}

@Composable
private fun BackButton(onClick: () -> Unit) {
    Box(modifier = Modifier.statusBarsPadding().padding(top = 8.dp, start = 8.dp)) {
        Image(
            painter = painterResource(R.drawable.arrowleft),
            contentDescription = "뒤로",
            colorFilter = ColorFilter.tint(Colors.textPrimary),
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Colors.commonWhite)
                .clickable(onClick = onClick)
                .padding(8.dp),
        )
    }
}

@Composable
private fun SheetBody(state: CourseResultState, height: Dp, onIntent: (CourseResultIntent) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().height(height)) {
        Header(state = state, onIntent = onIntent)
        when (state.load) {
            CourseResultLoad.LOADING -> Skeleton()
            CourseResultLoad.FAILED -> Failure(onRetry = { onIntent(CourseResultIntent.RetryClicked) })
            CourseResultLoad.LOADED -> Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            ) {
                CourseTimeline(
                    stops = state.timelineStops,
                    legs = state.timelineLegs,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
                Spacer(
                    modifier = Modifier.height(
                        if (state.showsNotifyButton) CTA_COVER_PADDING else 20.dp,
                    ),
                )
            }
        }
    }
}

@Composable
private fun Header(state: CourseResultState, onIntent: (CourseResultIntent) -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = state.course?.title.orEmpty(),
                style = Typography.title3SB,
                color = Colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            // 지난 데이트로 들어오면 수정을 안 낸다. 제목이 그 폭을 가져간다
            if (state.showsEditButton) {
                AppButton(
                    text = "수정",
                    onClick = { onIntent(CourseResultIntent.EditClicked) },
                    variant = AppButtonVariant.OUTLINED,
                    size = AppButtonSize.SM,
                )
            }
        }
        state.summaryText?.let {
            Text(text = it, style = Typography.body2M, color = Colors.textTertiary)
        }
    }
}

@Composable
private fun Skeleton() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        repeat(SKELETON_ROW_COUNT) {
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(SKELETON_ROW_HEIGHT)
                    .clip(RoundedCornerShape(12.dp)),
            )
        }
    }
}

@Composable
private fun Failure(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.placeempty),
            contentDescription = null,
            modifier = Modifier.size(120.dp),
        )
        Text(text = "코스를 불러오지 못했어요", style = Typography.headline, color = Colors.textPrimary)
        Text(text = "잠시 뒤 다시 시도해주세요", style = Typography.body2M, color = Colors.textTertiary)
        AppButton(
            text = "다시 시도",
            onClick = onRetry,
            variant = AppButtonVariant.OUTLINED,
            size = AppButtonSize.MD,
        )
    }
}
