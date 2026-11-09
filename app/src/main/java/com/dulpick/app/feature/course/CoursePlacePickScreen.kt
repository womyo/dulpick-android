package com.dulpick.app.feature.course

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dulpick.app.R
import com.dulpick.app.core.map.KakaoMapView
import com.dulpick.app.core.map.MapCameraRequest
import com.dulpick.app.core.map.MapPin
import com.dulpick.app.core.mvi.CollectSideEffect
import com.dulpick.app.domain.place.PlaceOwnership
import com.dulpick.app.domain.place.SavedPlace
import com.dulpick.app.feature.course.component.CoursePlaceRow
import com.dulpick.app.feature.course.component.SAVED_PLACE_SEARCH_HEIGHT
import com.dulpick.app.feature.course.component.SavedPlaceSearchChip
import com.dulpick.app.feature.course.component.SavedPlaceSearchField
import com.dulpick.app.ui.component.CATEGORY_ORDER
import com.dulpick.app.ui.component.CATEGORY_PLACEHOLDER
import com.dulpick.app.ui.component.CATEGORY_UNFILTERED
import com.dulpick.app.ui.component.FilterDropdown
import com.dulpick.app.ui.component.OWNERSHIP_ORDER
import com.dulpick.app.ui.component.displayName
import com.dulpick.app.ui.map.numberedPin
import com.dulpick.app.ui.map.placePin
import com.dulpick.app.ui.map.COURSE_MINIMUM_SHEET_GAP
import com.dulpick.app.ui.map.COURSE_PICK_SHEET_SCREEN_RATIO
import com.dulpick.app.ui.map.MapBottomSheet
import com.dulpick.app.ui.map.MapSheetHeights
import com.dulpick.app.ui.map.mapSheetMetrics
import com.dulpick.app.ui.map.rememberMapBottomSheetState
import com.dulpick.app.ui.map.rememberSheetMover
import com.dulpick.app.ui.map.rememberMapSheetStep
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

// 지도 화면과 같은 시트 높이 비율
private const val SHEET_EXPANDED_FRACTION = 0.74f
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
    // 더하기로 열 때 이미 담긴 장소. 목록에서 빼고 지도에는 기존 코스로 그린다
    existing: List<ExistingCoursePlace> = emptyList(),
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
        viewModel.onIntent(CoursePlacePickIntent.Start(dateCourseId, mode, existing))
        viewModel.onIntent(CoursePlacePickIntent.MapAppeared)
    }
    BackHandler { viewModel.onIntent(CoursePlacePickIntent.BackClicked) }

    val sheetState = rememberMapBottomSheetState()
    val sheetStep = rememberMapSheetStep()
    // 시트가 보이는 높이를 재는 기준. 목록 뷰포트가 이 안에 든다
    var rootHeightPx by remember { mutableStateOf(0) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Colors.bgDefault)
            .onSizeChanged { rootHeightPx = it.height },
    ) {
        val containerHeight = maxHeight
        val sheet = mapSheetMetrics(containerHeight, middleRatio = COURSE_PICK_SHEET_SCREEN_RATIO)
        val heights = MapSheetHeights(
            collapsed = sheet.collapsedHeight,
            expanded = sheetExpandedHeight,
            minimum = sheetStep.minimumHeight(gap = COURSE_MINIMUM_SHEET_GAP),
        )
        val moveSheet = rememberSheetMover(sheetState, heights, containerHeight)
        Box(modifier = Modifier.fillMaxSize()) {
            KakaoMapView(
                modifier = Modifier.fillMaxSize(),
                pins = coursePins(state),
                routes = state.routes,
                userLocation = state.userLocation,
                // 핀을 시트 위 영역의 초점에 둔다. 화면 한가운데면 시트가 가린다
                collapsedSheetTopPx = sheet.collapsedTopPx,
                cameraRequest = MapCameraRequest(state.camera, state.cameraNonce),
                onPinTap = { viewModel.onIntent(CoursePlacePickIntent.PinTapped(it)) },
            )
            BackButton(onClick = { viewModel.onIntent(CoursePlacePickIntent.BackClicked) })
            MapBottomSheet(state = sheetState, heights = heights, containerHeight = containerHeight) {
                SheetBody(
                    state = state,
                    height = sheetExpandedHeight,
                    rootHeightPx = rootHeightPx,
                    onCollapse = { moveSheet.stepDown() },
                    onIntent = viewModel::onIntent,
                    onHeaderHeightChange = { height -> sheetStep.headerHeight = height },
                    onMenuOpened = moveSheet::raiseForMenu,
                )
            }
        }

        BuildCta(
            title = state.ctaTitle,
            isEnabled = state.isCtaEnabled,
            onClick = { viewModel.onIntent(CoursePlacePickIntent.BuildClicked) },
            onHeightChange = { height -> sheetStep.obstacleHeight = height },
            modifier = Modifier.align(Alignment.BottomCenter),
        )

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

// 기존 코스 핀, 목록의 카테고리 핀, 고른 숫자 핀 순서다.
// 겹침은 배열 순서가 아니라 핀 종류가 정한다 — 숫자 핀이 카테고리 핀 위에 앉는다.
// 숫자 핀은 필터를 타지 않는다. 목록에서 사라져도 핀으로 뺄 수 있어야 한다 (iOS markers 대응)
@Composable
private fun coursePins(state: CoursePlacePickState): List<MapPin> {
    val context = LocalContext.current
    val existingPins = state.existingPlaces.flatMapIndexed { index, stop ->
        listOf(
            placePin(context, CourseMarkerId.existingCategory(stop.id), stop.coordinate, stop.category),
            numberedPin(
                context,
                CourseMarkerId.existingNumbered(stop.id),
                stop.coordinate,
                index + 1,
            ),
        )
    }
    val categoryPins = state.filteredPlaces.map {
        placePin(context, it.id, it.place.coordinate, it.place.category)
    }
    val numberedPins = state.selectedPlaces.mapIndexed { index, candidate ->
        numberedPin(
            context,
            CourseMarkerId.numbered(candidate.id),
            candidate.place.coordinate,
            state.existingPlaces.size + index + 1,
        )
    }
    return existingPins + categoryPins + numberedPins
}

// 화면 아래에 고정된 CTA. 시트를 올려도 자리가 안 바뀐다
@Composable
private fun BuildCta(
    title: String,
    isEnabled: Boolean,
    onClick: () -> Unit,
    onHeightChange: (Dp) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    Column(modifier = modifier.onSizeChanged { onHeightChange(with(density) { it.height.toDp() }) }) {
        CtaContainer {
            AppButton(
                text = title,
                onClick = onClick,
                variant = AppButtonVariant.DARK,
                size = AppButtonSize.XL,
                enabled = isEnabled,
                modifier = Modifier.fillMaxWidth(),
            )
        }
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
@Suppress("LongParameterList")
private fun SheetBody(
    state: CoursePlacePickState,
    height: androidx.compose.ui.unit.Dp,
    rootHeightPx: Int,
    onCollapse: () -> Unit,
    onIntent: (CoursePlacePickIntent) -> Unit,
    onHeaderHeightChange: (androidx.compose.ui.unit.Dp) -> Unit,
    onMenuOpened: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().height(height)) {
        Header(
            state = state,
            onIntent = onIntent,
            onHeightChange = onHeaderHeightChange,
            onMenuOpened = onMenuOpened,
        )
        when (state.listContent) {
            CoursePlaceListContent.SKELETON -> Skeleton()
            CoursePlaceListContent.LOAD_FAILED -> Failure(
                onRetry = { onIntent(CoursePlacePickIntent.RetryClicked) },
            )
            CoursePlaceListContent.NO_SAVED_PLACE ->
                EmptyState("저장한 장소가 없어요", "마음에 드는 장소를 저장해보세요")
            CoursePlaceListContent.NO_MATCH ->
                EmptyState("조건에 맞는 장소가 없어요", "필터를 바꿔보세요")
            CoursePlaceListContent.NO_SEARCH_RESULT ->
                EmptyState("검색 결과가 없어요", "다른 검색어를 입력해주세요")
            CoursePlaceListContent.SEARCH_FAILED ->
                EmptyState("검색에 실패했어요", "잠시 뒤 다시 시도해주세요")
            CoursePlaceListContent.LIST -> PlaceList(
                state = state,
                rootHeightPx = rootHeightPx,
                onCollapse = onCollapse,
                onIntent = onIntent,
            )
        }
    }
}

@Composable
private fun Header(
    state: CoursePlacePickState,
    onIntent: (CoursePlacePickIntent) -> Unit,
    onHeightChange: (androidx.compose.ui.unit.Dp) -> Unit,
    onMenuOpened: () -> Unit,
) {
    val density = LocalDensity.current
    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .padding(bottom = 8.dp)
            .onSizeChanged { onHeightChange(with(density) { it.height.toDp() }) },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = "저장한 장소", style = Typography.title3SB, color = Colors.textPrimary)
        FilterRow(state = state, onIntent = onIntent, onMenuOpened = onMenuOpened)
    }
}

// 검색창이 열리면 「검색」 칩이 줄 너비로 늘어나 검색창이 되고 나머지 칩은 사라진다.
// 두 모양이 같은 높이라 바뀌어도 머리 높이가 그대로다
@Composable
private fun FilterRow(
    state: CoursePlacePickState,
    onIntent: (CoursePlacePickIntent) -> Unit,
    onMenuOpened: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(SAVED_PLACE_SEARCH_HEIGHT),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (state.search.isFieldOpen) {
            SavedPlaceSearchField(
                query = state.search.query,
                onQueryChange = { onIntent(CoursePlacePickIntent.SearchQueryChanged(it)) },
                onFocusLost = { onIntent(CoursePlacePickIntent.SearchFieldDismissed) },
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "취소",
                style = Typography.body2SB,
                color = Colors.textTertiary,
                modifier = Modifier
                    .clickable { onIntent(CoursePlacePickIntent.SearchCancelClicked) }
                    .padding(horizontal = 6.dp),
            )
            return@Row
        }
        SavedPlaceSearchChip(
            query = state.search.query,
            isEnabled = state.isSearchAvailable,
            onClick = {
                onMenuOpened()
                onIntent(CoursePlacePickIntent.SearchChipClicked)
            },
            onClear = { onIntent(CoursePlacePickIntent.SearchChipClearClicked) },
            modifier = Modifier.weight(1f, fill = false),
        )
        if (state.isCoupleConnected) {
            FilterDropdown(
                label = state.selectedOwnership.displayName(),
                isActive = state.selectedOwnership != PlaceOwnership.TOGETHER,
                options = OWNERSHIP_ORDER.map { it.displayName() },
                selectedIndex = OWNERSHIP_ORDER.indexOf(state.selectedOwnership),
                onSelect = { onIntent(CoursePlacePickIntent.OwnershipSelected(OWNERSHIP_ORDER[it])) },
                onExpand = onMenuOpened,
            )
        }
        FilterDropdown(
            label = state.selectedCategory?.displayName() ?: CATEGORY_PLACEHOLDER,
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

@Composable
private fun PlaceList(
    state: CoursePlacePickState,
    rootHeightPx: Int,
    onCollapse: () -> Unit,
    onIntent: (CoursePlacePickIntent) -> Unit,
) {
    val places = state.filteredPlaces
    val listState = rememberLazyListState()
    ScrollToRequestedRow(request = state.rowScrollRequest, places = places, listState = listState)
    LazyColumn(
        state = listState,
        userScrollEnabled = false,
        modifier = Modifier
            .fillMaxWidth()
            .sheetVisibleHeight(rootHeightPx)
            .sheetContentScroll(listState, onPullDownAtTop = onCollapse),
    ) {
        itemsIndexed(places, key = { _, place -> place.id }) { index, place ->
            // 검색 결과는 끝에 닿으면 다음 장을 부른다
            if (state.shouldLoadMoreSearch(index, places.size)) {
                LaunchedEffect(index, places.size) { onIntent(CoursePlacePickIntent.SearchReachedEnd) }
            }
            CoursePlaceRow(
                place = place,
                number = state.badgeNumber(place.id),
                showsDivider = index != places.lastIndex,
                onClick = { onIntent(CoursePlacePickIntent.RowClicked(place.id)) },
            )
        }
        // 마지막 행이 CTA 버튼에 가리지 않게 비운다
        item { Spacer(modifier = Modifier.height(CTA_COVER_PADDING)) }
    }
}

// 핀을 누르면 리듀서가 남긴 요청대로 목록을 그 행까지 올린다.
// 요청 번호까지 보므로 같은 행을 다시 눌러도 다시 맞춘다. 시트 높이는 건드리지 않는다
@Composable
private fun ScrollToRequestedRow(
    request: RowScrollRequest?,
    places: List<SavedPlace>,
    listState: LazyListState,
) {
    LaunchedEffect(request) {
        val target = request ?: return@LaunchedEffect
        val index = places.indexOfFirst { it.id == target.placeId }
        if (index >= 0) listState.animateScrollToItem(index)
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
private fun EmptyState(title: String, message: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.placeempty),
            contentDescription = null,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = title, style = Typography.title3SB, color = Colors.textPrimary)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = message, style = Typography.body1M, color = Colors.textTertiary)
    }
}
