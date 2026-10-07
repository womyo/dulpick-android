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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dulpick.app.R
import com.dulpick.app.core.map.KakaoMapView
import com.dulpick.app.core.map.MapCamera
import com.dulpick.app.core.map.MapPin
import com.dulpick.app.core.mvi.CollectSideEffect
import com.dulpick.app.domain.place.PlaceOwnership
import com.dulpick.app.domain.place.SavedPlace
import com.dulpick.app.feature.course.component.CoursePlaceRow
import com.dulpick.app.ui.map.candidatePin
import com.dulpick.app.ui.component.CATEGORY_ORDER
import com.dulpick.app.ui.component.CATEGORY_UNFILTERED
import com.dulpick.app.ui.component.FilterDropdown
import com.dulpick.app.ui.component.OWNERSHIP_ORDER
import com.dulpick.app.ui.component.displayName
import com.dulpick.app.ui.map.placePin
import com.dulpick.app.ui.map.rememberMapSheetState
import com.dulpick.app.ui.component.AppButton
import com.dulpick.app.ui.component.AppButtonSize
import com.dulpick.app.ui.component.AppButtonVariant
import com.dulpick.app.ui.component.AppToast
import com.dulpick.app.ui.component.CtaContainer
import com.dulpick.app.ui.component.ShimmerBox
import com.dulpick.app.ui.component.sheetContentScroll
import com.dulpick.app.ui.component.sheetVisibleHeight
import com.dulpick.app.ui.theme.Colors
import com.dulpick.app.ui.theme.Typography
import kotlinx.coroutines.launch

// 지도 화면과 같은 시트 높이 비율
private const val SHEET_PEEK_FRACTION = 0.42f
private const val SHEET_EXPANDED_FRACTION = 0.74f
private val SHEET_CORNER_RADIUS = 32.dp
private const val SKELETON_ROW_COUNT = 3
private val SKELETON_ROW_HEIGHT = 64.dp

// 목록 마지막 행이 CTA 버튼 위로 올라오게 비우는 높이 (버튼 56 + 아래 여백 20 + 간격 20)
private val CTA_COVER_PADDING = 96.dp

// 지도 탭에는 탭바가 있고 이 화면엔 없다. 펼침 높이를 맞추려면 그만큼 더해야 한다
private val TAB_BAR_HEIGHT = 80.dp

// 코스에 담을 장소를 고르는 화면. 고른 순서가 곧 번호다 (iOS CoursePlacePickView 대응)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongParameterList")
fun CoursePlacePickScreen(
    onBack: () -> Unit,
    onSessionExpired: () -> Unit,
    // 코스를 처음 짤 때만 있다. 더하기로 열면 null 이다
    dateCourseId: String? = null,
    mode: CoursePlacePickMode = CoursePlacePickMode.CREATE,
    // 더하기로 열 때 이미 담긴 장소. 목록에서 뺀다
    excluding: List<String> = emptyList(),
    onBuilt: (dateCourseId: String) -> Unit = {},
    // 더하기로 열렸을 때 고른 장소를 코스 수정 화면으로 돌려준다
    onPicked: (List<SavedPlace>) -> Unit = {},
    viewModel: CoursePlacePickViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var toastMessage by remember { mutableStateOf<String?>(null) }
    val screenHeight = LocalConfiguration.current.screenHeightDp
    val sheetExpandedHeight = (screenHeight * SHEET_EXPANDED_FRACTION).dp + TAB_BAR_HEIGHT +
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    CollectSideEffect(viewModel.sideEffect) { effect ->
        when (effect) {
            CoursePlacePickSideEffect.Dismissed -> onBack()
            CoursePlacePickSideEffect.SessionExpired -> onSessionExpired()
            is CoursePlacePickSideEffect.ShowToast -> toastMessage = effect.message
            is CoursePlacePickSideEffect.BuildRequested -> onBuilt(effect.dateCourseId)
            is CoursePlacePickSideEffect.PlacesPicked -> onPicked(effect.places)
        }
    }
    LaunchedEffect(dateCourseId, mode) {
        viewModel.onIntent(CoursePlacePickIntent.Start(dateCourseId, mode, excluding))
    }
    BackHandler { viewModel.onIntent(CoursePlacePickIntent.BackClicked) }

    val scaffoldState = rememberMapSheetState()
    val scope = rememberCoroutineScope()
    // 시트가 보이는 높이를 재는 기준. 목록 뷰포트가 이 안에 든다
    var rootHeightPx by remember { mutableStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Colors.bgDefault)
            .onSizeChanged { rootHeightPx = it.height },
    ) {
        BottomSheetScaffold(
            scaffoldState = scaffoldState,
            sheetPeekHeight = (screenHeight * SHEET_PEEK_FRACTION).dp,
            sheetContainerColor = Colors.commonWhite,
            containerColor = Color.Transparent,
            sheetTonalElevation = 0.dp,
            sheetShape = RoundedCornerShape(topStart = SHEET_CORNER_RADIUS, topEnd = SHEET_CORNER_RADIUS),
            sheetContent = {
                SheetBody(
                    state = state,
                    height = sheetExpandedHeight,
                    rootHeightPx = rootHeightPx,
                    onCollapse = { scope.launch { scaffoldState.bottomSheetState.partialExpand() } },
                    onIntent = viewModel::onIntent,
                )
            },
        ) {
            val pins = coursePins(state)
            Box(modifier = Modifier.fillMaxSize()) {
                KakaoMapView(
                    modifier = Modifier.fillMaxSize(),
                    pins = pins,
                    camera = pins.firstOrNull()
                        ?.let { MapCamera(it.coordinate, MapCamera.MULTI_PLACE_ZOOM) }
                        ?: MapCamera.SEOUL_CITY_HALL,
                    // 핀 탭도 목록 행 탭과 같은 토글이다 (iOS markerTapped 대응)
                    onPinTap = { viewModel.onIntent(CoursePlacePickIntent.PlaceToggled(it)) },
                )
                BackButton(onClick = { viewModel.onIntent(CoursePlacePickIntent.BackClicked) })
            }
        }

        // 화면 아래에 고정된 CTA. 시트를 올려도 자리가 안 바뀐다
        Column(modifier = Modifier.align(Alignment.BottomCenter)) {
            CtaContainer {
                AppButton(
                    text = state.ctaTitle,
                    onClick = { viewModel.onIntent(CoursePlacePickIntent.BuildClicked) },
                    variant = AppButtonVariant.DARK,
                    size = AppButtonSize.XL,
                    enabled = state.isCtaEnabled,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        AppToast(message = toastMessage, onDismiss = { toastMessage = null }, bottomInset = 100)
    }

    state.conflictMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { viewModel.onIntent(CoursePlacePickIntent.ConflictDismissed) },
            confirmButton = {
                TextButton(onClick = { viewModel.onIntent(CoursePlacePickIntent.ConflictDismissed) }) {
                    Text(text = "확인")
                }
            },
            text = { Text(text = message) },
            containerColor = Colors.commonWhite,
        )
    }
}

// 카테고리 핀을 먼저 두고 고른 물방울을 뒤에 둔다. 지도가 배열 순서로 그려 물방울이 위에 온다.
// 고른 물방울은 필터를 타지 않는다 — 목록에서 사라져도 핀으로 해제할 수 있어야 한다 (iOS markers 대응)
@Composable
private fun coursePins(state: CoursePlacePickState): List<MapPin> {
    val context = LocalContext.current
    val categoryPins = state.filteredPlaces.map {
        placePin(context, it.id, it.place.coordinate, it.place.category)
    }
    val selectedPins = state.places
        .filter { it.id in state.selectedPlaceIds }
        .map { candidatePin(context, it.id, it.place.coordinate) }
    return categoryPins + selectedPins
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
private fun SheetBody(
    state: CoursePlacePickState,
    height: androidx.compose.ui.unit.Dp,
    rootHeightPx: Int,
    onCollapse: () -> Unit,
    onIntent: (CoursePlacePickIntent) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().height(height)) {
        Header(state = state, onIntent = onIntent)
        when {
            state.load == CoursePlaceLoad.LOADING -> Skeleton()
            state.load == CoursePlaceLoad.FAILED -> Failure(
                onRetry = { onIntent(CoursePlacePickIntent.RetryClicked) },
            )
            state.isEmpty -> EmptyState(hasNoSavedPlace = state.hasNoSavedPlace)
            else -> PlaceList(
                state = state,
                rootHeightPx = rootHeightPx,
                onCollapse = onCollapse,
                onIntent = onIntent,
            )
        }
    }
}

@Composable
private fun Header(state: CoursePlacePickState, onIntent: (CoursePlacePickIntent) -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = "저장한 장소", style = Typography.title3SB, color = Colors.textPrimary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.isCoupleConnected) {
                FilterDropdown(
                    label = state.selectedOwnership.displayName(),
                    isActive = state.selectedOwnership != PlaceOwnership.TOGETHER,
                    options = OWNERSHIP_ORDER.map { it.displayName() },
                    selectedIndex = OWNERSHIP_ORDER.indexOf(state.selectedOwnership),
                    onSelect = { onIntent(CoursePlacePickIntent.OwnershipSelected(OWNERSHIP_ORDER[it])) },
                )
            }
            FilterDropdown(
                label = state.selectedCategory?.displayName() ?: CATEGORY_UNFILTERED,
                isActive = state.selectedCategory != null,
                options = listOf(CATEGORY_UNFILTERED) + CATEGORY_ORDER.map { it.displayName() },
                selectedIndex = state.selectedCategory?.let { CATEGORY_ORDER.indexOf(it) + 1 } ?: 0,
                onSelect = {
                    onIntent(
                        CoursePlacePickIntent.CategorySelected(
                            if (it == 0) null else CATEGORY_ORDER[it - 1],
                        ),
                    )
                },
            )
        }
    }
}

@Composable
private fun PlaceList(
    state: CoursePlacePickState,
    rootHeightPx: Int,
    onCollapse: () -> Unit,
    onIntent: (CoursePlacePickIntent) -> Unit,
) {
    val places = state.filteredPlaces
    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        userScrollEnabled = false,
        modifier = Modifier
            .fillMaxWidth()
            .sheetVisibleHeight(rootHeightPx)
            .sheetContentScroll(listState, onPullDownAtTop = onCollapse),
    ) {
        itemsIndexed(places, key = { _, place -> place.id }) { index, place ->
            CoursePlaceRow(
                place = place,
                number = state.badgeNumber(place.id),
                showsDivider = index != places.lastIndex,
                onClick = { onIntent(CoursePlacePickIntent.PlaceToggled(place.id)) },
            )
        }
        // 마지막 행이 CTA 버튼에 가리지 않게 비운다
        item { Spacer(modifier = Modifier.height(CTA_COVER_PADDING)) }
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
        )
        Text(text = "장소를 불러오지 못했어요", style = Typography.title3SB, color = Colors.textPrimary)
        Text(text = "잠시 뒤 다시 시도해주세요", style = Typography.body1M, color = Colors.textTertiary)
        AppButton(
            text = "다시 시도",
            onClick = onRetry,
            variant = AppButtonVariant.OUTLINED,
            size = AppButtonSize.MD,
        )
    }
}

@Composable
private fun EmptyState(hasNoSavedPlace: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.placeempty),
            contentDescription = null,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (hasNoSavedPlace) "저장한 장소가 없어요" else "조건에 맞는 장소가 없어요",
            style = Typography.title3SB,
            color = Colors.textPrimary,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (hasNoSavedPlace) "마음에 드는 장소를 저장해보세요" else "필터를 바꿔보세요",
            style = Typography.body1M,
            color = Colors.textTertiary,
        )
    }
}
