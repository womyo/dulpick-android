package com.dulpick.app.feature.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.dulpick.app.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dulpick.app.core.map.KakaoMapView
import com.dulpick.app.core.mvi.CollectSideEffect
import com.dulpick.app.domain.place.Coordinate
import com.dulpick.app.domain.place.Place
import com.dulpick.app.domain.place.PlaceCategory
import com.dulpick.app.domain.place.PlaceOwnership
import com.dulpick.app.feature.map.component.CATEGORY_ORDER
import com.dulpick.app.feature.map.component.CATEGORY_UNFILTERED
import com.dulpick.app.feature.map.component.CategoryChipBar
import com.dulpick.app.feature.map.component.FilterDropdown
import com.dulpick.app.feature.map.component.MapSearchBar
import com.dulpick.app.feature.placedetail.MapSearchReturnArg
import com.dulpick.app.feature.placedetail.PlaceDetailSheet
import com.dulpick.app.feature.map.component.OWNERSHIP_ORDER
import com.dulpick.app.feature.map.component.PlaceAliasSheet
import com.dulpick.app.feature.map.component.PlaceListRow
import com.dulpick.app.feature.map.component.displayName
import com.dulpick.app.ui.component.AppButton
import com.dulpick.app.ui.component.AppButtonSize
import com.dulpick.app.ui.component.AppButtonVariant
import com.dulpick.app.ui.component.AppToast
import com.dulpick.app.ui.component.iconRes
import com.dulpick.app.ui.component.pinRes
import androidx.compose.ui.text.style.TextOverflow
import com.dulpick.app.ui.theme.Colors
import com.dulpick.app.ui.theme.Typography
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import kotlinx.coroutines.delay
import kotlin.math.abs

// 지도 탭. 저장 장소를 지도에 핀으로 찍고, 아래 바텀시트에 목록으로 보여준다 (iOS MapView 대응)
private val SEOUL_CITY_HALL = LatLng.from(37.5666, 126.9784)
// 카메라 도달 검증 재시도. 엔진 재개 직후엔 명령이 유실될 수 있어 앉을 때까지 다시 보낸다
private const val RENDER_MAX_TRIES = 10
private const val RENDER_VERIFY_MS = 300L
private const val CAMERA_EPSILON = 1e-4
// 선택 마커가 카테고리 배지(기본 rank 0) 위에 항상 오도록 하는 rank (iOS selected rank 1 대응)
private const val SELECTED_MARKER_RANK = 1L
// iOS multiPlaceZoom. 시작·저장 목록·검색 결과·상세 모두 이 배율을 쓴다(단일 장소도 14)
private const val DEFAULT_ZOOM_LEVEL = 14
// 접힘(기본) 높이 = 화면 높이의 42% (iOS collapsedScreenRatio 40~45% 범위).
// 펼침은 모든 시트(저장목록·검색결과·상세) 공통. 화면을 다 덮지 않도록 상단(검색바)을 남긴다
private const val SHEET_PEEK_FRACTION = 0.42f
private const val SHEET_EXPANDED_FRACTION = 0.74f
private val SHEET_CORNER_RADIUS = 32.dp

// 화면에 떠 있는 토스트. isError 면 에러 아이콘을 붙인다
private data class MapToast(val message: String, val isError: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    // 탭바가 차지하는 높이. 탭바가 감춰져 화면이 길어진 만큼 지도 뷰가 아래를 비워,
    // 시트가 바뀌어도 지도 뷰 크기가 변하지 않게 한다
    tabBarHeight: Dp = 0.dp,
    // 탭 밖에서 들고 있는 시트 상태. 여기서 만들면 탭을 옮길 때마다 새로 만들어져 시트가 위에서 떨어진다
    sheetState: BottomSheetScaffoldState = rememberMapSheetState(),
    onSessionExpired: () -> Unit,
    onOpenSearch: () -> Unit,
    // 검색 화면에서 되돌아온 검색 결과(query+places JSON). 검색이 pop 되며 지도로 전달된다
    pendingSearchArg: String? = null,
    onSearchConsumed: () -> Unit = {},
    // 검색 결과 모드에서 검색바 뒤로 → 그 검색어로 검색 화면을 다시 연다
    onReopenSearch: (String) -> Unit = {},
    // 홈 등 다른 탭에서 넘어온 저장 장소. 지도 탭 진입 시 그 장소 상세를 연다
    pendingPlace: Place? = null,
    onPlaceConsumed: () -> Unit = {},
    // 탐색 검색에서 넘어온 장소. 검색바 없이 그 장소 핀과 상세만 보여준다
    pendingContentDetail: DetailTarget? = null,
    onContentDetailConsumed: () -> Unit = {},
    // 상세 전용(content) 모드 닫힘 → 온 곳(탐색 검색)으로 되돌린다
    onCloseContentDetail: () -> Unit = {},
    // 저장 목록이 아닌 시트(검색 결과·장소 상세)가 떠 있는지 위로 알린다.
    // 탭 컨테이너가 이 값으로 탭바를 감춘다
    onTabBarHiddenChange: (Boolean) -> Unit = {},
    // 탭바가 없는 화면(상세 시트가 뜬 지도)에서 펼침 높이에 더해 줄 값.
    // 탭바가 차지하던 만큼 더해야 시트가 탭바 있을 때와 같은 높이까지 펼쳐진다(접힘 높이는 그대로)
    sheetBottomInset: Dp = 0.dp,
    viewModel: MapViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var toast by remember { mutableStateOf<MapToast?>(null) }
    val screenHeight = LocalConfiguration.current.screenHeightDp
    // 지도 뷰는 시트 본문 안에 있어야 터치를 받는다(시트의 Material Surface 가 터치를 흡수한다).
    // 그래서 이 화면이 지도를 들고 있고, 준 KakaoMap 과 재개 신호를 여기서 기억한다
    var kakaoMap by remember { mutableStateOf<KakaoMap?>(null) }
    var mapRevision by remember { mutableStateOf(0) }
    // 저장 목록이 아닌 시트(검색 결과·장소 상세)가 뜨면 탭바가 사라져 이 화면이 그만큼 길어진다
    val tabBarHidden = state.detail != null || state.searchResult != null

    CollectSideEffect(viewModel.sideEffect) { effect ->
        when (effect) {
            MapSideEffect.SessionExpired -> onSessionExpired()
            is MapSideEffect.ShowToast -> toast = MapToast(effect.message, effect.isError)
        }
    }
    LaunchedEffect(Unit) { viewModel.onIntent(MapIntent.OnAppear) }

    ConsumeExternalInputs(
        pendingPlace = pendingPlace,
        onPlaceConsumed = onPlaceConsumed,
        pendingContentDetail = pendingContentDetail,
        onContentDetailConsumed = onContentDetailConsumed,
        pendingSearchArg = pendingSearchArg,
        onSearchConsumed = onSearchConsumed,
        onIntent = viewModel::onIntent,
    )

    ReportTabBarHidden(tabBarHidden, onTabBarHiddenChange)
    HandlePinTaps(kakaoMap, state, viewModel::onIntent)

    val dismissDetail: (DetailTarget) -> Unit =
        { it.dismiss(viewModel::onIntent, onCloseContentDetail) }
    // 뒤로가기: 상세가 열려 있으면 상세를 닫고, 검색 결과 모드면 저장 모드로 돌아간다
    BackHandler(enabled = state.detail != null || state.searchResult != null) {
        val detail = state.detail
        if (detail != null) dismissDetail(detail) else viewModel.onIntent(MapIntent.ClearSearch)
    }

    val sheetAlpha = rememberSheetAlpha(sheetState)

    Box(modifier = Modifier.fillMaxSize()) {
        BottomSheetScaffold(
            modifier = Modifier.alpha(sheetAlpha),
            scaffoldState = sheetState,
            sheetPeekHeight = (screenHeight * SHEET_PEEK_FRACTION).dp,
            sheetContainerColor = Colors.commonWhite,
            // 지도가 이 화면 아래층(탭 컨테이너)에 있으므로 배경을 비워 지도가 비치게 한다
            containerColor = Color.Transparent,
            // 기본 tonalElevation 이 흰색에 톤 오버레이를 얹어 색이 뜨므로 끈다
            sheetTonalElevation = 0.dp,
            sheetShape = RoundedCornerShape(topStart = SHEET_CORNER_RADIUS, topEnd = SHEET_CORNER_RADIUS),
            sheetContent = {
                MapSheet(
                    state = state,
                    sheetHeight = (screenHeight * SHEET_EXPANDED_FRACTION).dp + sheetBottomInset,
                    onIntent = viewModel::onIntent,
                    onSessionExpired = onSessionExpired,
                    onCloseDetail = dismissDetail,
                )
            },
        ) {
            MapBody(
                // 탭바가 사라져 길어진 만큼 비워 두면 지도 뷰 크기가 늘 같다
                mapBottomInset = if (tabBarHidden) tabBarHeight else 0.dp,
                onMapReady = { kakaoMap = it },
                onResumed = { mapRevision++ },
                searchQuery = state.searchResult?.displayQuery,
                // content 모드는 검색바·칩 없이 지도 위 상세만 보인다 (iOS isContentMode 대응)
                hideTopBar = state.detail?.contentMode == true,
                selectedCategory = state.selectedCategory,
                actions = MapTopBarActions(
                    onOpenSearch = onOpenSearch,
                    // 재검색: 입력 화면으로 이동하며 지도의 검색 상태를 비운다.
                    // 안 비우면 입력 화면에서 뒤로갈 때 네비가 얼린 상세를 복원해 무한 루프가 된다
                    onReopenSearch = {
                        state.searchResult?.let { onReopenSearch(it.searchQuery) }
                        viewModel.onIntent(MapIntent.ClearSearch)
                    },
                    onClearSearch = { viewModel.onIntent(MapIntent.ClearSearch) },
                    onCategorySelected = { viewModel.onIntent(MapIntent.CategorySelected(it)) },
                ),
            )
        }

        AppToast(
            message = toast?.message,
            onDismiss = { toast = null },
            bottomInset = 16,
            iconRes = if (toast?.isError == true) R.drawable.error else null,
        )
    }

    state.aliasEdit?.let { edit -> AliasEditModal(edit = edit, onIntent = viewModel::onIntent) }

    // 상세/검색결과/저장 순으로 핀과 카메라를 맞춘다.
    // 지도 준비·재개(mapRevision)·저장목록·검색결과·상세 변경마다 다시 그린다
    LaunchedEffect(kakaoMap, mapRevision, state.filteredPlaces, state.searchResult, state.detail) {
        renderMapUntilSettled(context, kakaoMap ?: return@LaunchedEffect, state)
    }
}

// 엔진 준비·재개 직후에는 카메라·라벨 명령이 유실될 수 있다(카메라가 기본 위치에 남아 빈 지역만 보임).
// 카메라가 목표에 앉은 걸 확인할 때까지 재시도한다
private suspend fun renderMapUntilSettled(context: Context, map: KakaoMap, state: MapState) {
    repeat(RENDER_MAX_TRIES) {
        val target = renderMap(context, map, state)
        delay(RENDER_VERIFY_MS)
        val pos = map.cameraPosition?.position
        val arrived = pos != null &&
            abs(pos.latitude - target.latitude) < CAMERA_EPSILON &&
            abs(pos.longitude - target.longitude) < CAMERA_EPSILON
        if (arrived) return
    }
}

// 기본 핀(검색결과면 결과 장소들, 아니면 저장 장소 전체)은 상세가 열려도 지우지 않고,
// 상세가 열린 장소 위에 선택 마커만 얹는다(iOS markers 대응). 카메라는 상세면 그 장소, 아니면 첫 핀.
// 목표 좌표를 되돌려 호출부가 도달을 검증한다
private fun renderMap(context: Context, map: KakaoMap, state: MapState): LatLng {
    val detail = state.detail
    val searchResult = state.searchResult
    val basePins = when {
        // content 모드는 그 장소 하나만 찍는다 (iOS mode=.content([place]) 대응)
        detail != null && detail.contentMode ->
            listOf(MapPin(detail.place.id, detail.place.coordinate, detail.place.category))
        searchResult != null -> searchResult.places.map { MapPin(it.id, it.coordinate, it.category) }
        else -> state.filteredPlaces.map { MapPin(it.place.id, it.place.coordinate, it.place.category) }
    }
    renderPins(context, map, basePins, selected = detail?.place?.coordinate)
    // 카메라: 상세 > 첫 핀 > 서울 시청 (iOS overview 대응)
    val focus = detail?.place?.coordinate ?: basePins.firstOrNull()?.coordinate
    val center = focus?.let { LatLng.from(it.latitude, it.longitude) } ?: SEOUL_CITY_HALL
    map.moveCamera(CameraUpdateFactory.newCenterPosition(center, DEFAULT_ZOOM_LEVEL))
    return center
}

// 아래로 당기면 접힘 밑으로도 손가락 따라 내려가되(보통 시트처럼), 놓으면 접힘으로 튕겨 올라오고
// 절대 사라지지 않는다. Hidden 앵커는 살려 아래 움직임을 허용하고(skipHiddenState=false),
// confirmValueChange 로 Hidden 안착만 거부해 복귀시킨다
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberMapSheetState(): BottomSheetScaffoldState {
    return rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded,
            skipHiddenState = false,
            confirmValueChange = { it != SheetValue.Hidden },
        ),
    )
}

// 시트는 첫 레이아웃 전까지 위치가 없어 화면 맨 위에 그려진다. 그 한 프레임만 감추고,
// 위치가 정해진 뒤로는(아래에서 올라오는 동안에도) 계속 보여준다
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun rememberSheetAlpha(sheetState: BottomSheetScaffoldState): Float {
    // 위치가 정해지기 전에는 requireOffset 이 던진다
    val positioned = runCatching { sheetState.bottomSheetState.requireOffset() }.isSuccess
    var placed by remember { mutableStateOf(false) }
    LaunchedEffect(positioned) { if (positioned) placed = true }
    return if (placed) 1f else 0f
}

// 상세 닫기(X·뒤로가기 공용): content 모드면 온 곳(탐색 검색)으로 되돌리고, 아니면 상세만 내린다
private fun DetailTarget.dismiss(onIntent: (MapIntent) -> Unit, onCloseContentDetail: () -> Unit) {
    onIntent(MapIntent.CloseDetail)
    if (contentMode) onCloseContentDetail()
}

// 별칭 편집 모달 시트 (iOS PlaceAliasView 대응)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AliasEditModal(edit: AliasEdit, onIntent: (MapIntent) -> Unit) {
    ModalBottomSheet(
        onDismissRequest = { onIntent(MapIntent.AliasEditDismissed) },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Colors.commonWhite,
        // 기본 tonalElevation 이 흰색에 톤 오버레이를 얹어 회색으로 뜨므로 끈다
        tonalElevation = 0.dp,
    ) {
        PlaceAliasSheet(edit = edit, onSave = { onIntent(MapIntent.AliasSaveClicked(it)) })
    }
}

// 지도 핀 탭 → 그 장소 상세 (iOS markerTapped 대응).
// 선택 마커(물방울)에는 태그가 없어 탭해도 무시된다 — 이미 상세가 열린 장소다
@Composable
private fun HandlePinTaps(kakaoMap: KakaoMap?, state: MapState, onIntent: (MapIntent) -> Unit) {
    val current by rememberUpdatedState(state)
    LaunchedEffect(kakaoMap) {
        val map = kakaoMap ?: return@LaunchedEffect
        map.setOnLabelClickListener { _, _, label ->
            val intent = current.pinTapIntent(label.tag as? String)
            intent?.let(onIntent)
            intent != null
        }
    }
}

// 탭한 핀의 장소 상세를 연다. 저장 목록·검색 결과 모드 모두 리스트 행 탭과 같은 길을 쓴다.
// content 모드는 그 장소 상세가 이미 열려 있어 할 일이 없다 (iOS presentDetail(id) 대응)
private fun MapState.pinTapIntent(placeId: String?): MapIntent? {
    if (placeId == null || detail?.contentMode == true) return null
    val result = searchResult
    return if (result != null) {
        result.places.firstOrNull { it.id == placeId }?.let { MapIntent.SearchRowClicked(it) }
    } else {
        filteredPlaces.firstOrNull { it.id == placeId }?.let { MapIntent.OpenSavedDetail(it) }
    }
}

// 탭바를 감춰야 하는지 위로 알린다. 화면을 떠날 때는 내려 줘야 다른 탭에 탭바가 돌아온다
@Composable
private fun ReportTabBarHidden(hidden: Boolean, onChange: (Boolean) -> Unit) {
    LaunchedEffect(hidden) { onChange(hidden) }
    DisposableEffect(Unit) { onDispose { onChange(false) } }
}

// 다른 화면(홈 탭·탐색 검색·지도 검색)에서 넘어온 것들을 한 번씩 열고 소비 콜백을 부른다
@Suppress("LongParameterList")
@Composable
private fun ConsumeExternalInputs(
    pendingPlace: Place?,
    onPlaceConsumed: () -> Unit,
    pendingContentDetail: DetailTarget?,
    onContentDetailConsumed: () -> Unit,
    pendingSearchArg: String?,
    onSearchConsumed: () -> Unit,
    onIntent: (MapIntent) -> Unit,
) {
    // 홈 등에서 넘어온 저장 장소는 저장 모드에서 상세만 연다
    ConsumePendingPlace(pendingPlace, onPlaceConsumed) { onIntent(MapIntent.OpenPlaceDetail(it)) }

    // 탐색 검색에서 넘어온 장소는 검색바 없는 상세 전용(content) 모드로 연다
    LaunchedEffect(pendingContentDetail) {
        val target = pendingContentDetail ?: return@LaunchedEffect
        onIntent(MapIntent.OpenContentDetail(target.place, target.query))
        onContentDetailConsumed()
    }

    // 지도 검색에서 넘어온 결과는 검색 결과 모드로 올린다. selectedIndex 있으면 상세도 연다
    ConsumeSearchArg(pendingSearchArg, onSearchConsumed) { arg ->
        onIntent(
            MapIntent.EnterSearchResult(
                searchQuery = arg.searchQuery,
                displayQuery = arg.displayQuery,
                places = arg.places.map { it.toPlace() },
                selectedIndex = arg.selectedIndex,
            ),
        )
    }
}

// 다른 탭(홈 등)에서 넘어온 저장 장소를 한 번 열고 소비 콜백을 부른다
@Composable
private fun ConsumePendingPlace(place: Place?, onConsumed: () -> Unit, onOpen: (Place) -> Unit) {
    LaunchedEffect(place) {
        val target = place ?: return@LaunchedEffect
        onOpen(target)
        onConsumed()
    }
}

// 검색 화면에서 되돌아온 결과(JSON)를 한 번만 처리한다.
// savedStateHandle 은 상태라 재전달될 수 있어 nonce 로 같은 결과를 걸러낸다(방어)
@Composable
private fun ConsumeSearchArg(
    pendingSearchArg: String?,
    onConsumed: () -> Unit,
    onEnter: (MapSearchReturnArg) -> Unit,
) {
    var consumedNonce by rememberSaveable { mutableStateOf(0L) }
    LaunchedEffect(pendingSearchArg) {
        val arg = pendingSearchArg?.let(MapSearchReturnArg::decode) ?: return@LaunchedEffect
        if (arg.nonce == consumedNonce) return@LaunchedEffect
        consumedNonce = arg.nonce
        onEnter(arg)
        onConsumed()
    }
}

// 상단 검색바·필터 콜백 묶음 (파라미터 수 축소)
private class MapTopBarActions(
    val onOpenSearch: () -> Unit,
    val onReopenSearch: () -> Unit,
    val onClearSearch: () -> Unit,
    val onCategorySelected: (PlaceCategory?) -> Unit,
)

// 시트 콘텐츠 전환. 상세 > 검색결과 > 저장목록 순
@Composable
private fun MapSheet(
    state: MapState,
    sheetHeight: Dp,
    onIntent: (MapIntent) -> Unit,
    onSessionExpired: () -> Unit,
    onCloseDetail: (DetailTarget) -> Unit,
) {
    val detail = state.detail
    val searchResult = state.searchResult
    when {
        detail != null -> PlaceDetailSheet(
            target = detail.place,
            query = detail.query,
            serverPlaceId = detail.serverPlaceId,
            onClose = { onCloseDetail(detail) },
            onSessionExpired = onSessionExpired,
            modifier = Modifier.height(sheetHeight),
        )
        searchResult != null -> SearchResultSheet(
            result = searchResult,
            state = state,
            onIntent = onIntent,
            height = sheetHeight,
        )
        else -> MapSheetContent(
            state = state,
            onIntent = onIntent,
            height = sheetHeight,
        )
    }
}

// 상단 컨트롤(검색바·카테고리 칩바) 오버레이. 지도는 탭 컨테이너가 아래층에 그린다.
// 검색 결과 모드(searchQuery != null)면 검색바가 [뒤로][검색어 X]로 바뀌고 카테고리 칩은 감춘다
@Composable
@Suppress("LongParameterList")
private fun MapBody(
    mapBottomInset: Dp,
    onMapReady: (KakaoMap) -> Unit,
    onResumed: () -> Unit,
    searchQuery: String?,
    hideTopBar: Boolean,
    selectedCategory: PlaceCategory?,
    actions: MapTopBarActions,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // 지도는 시트 본문 안에 둔다. 시트(Material Surface)가 터치를 흡수하므로,
        // 밖에 두면 지도가 드래그·확대·핀 탭을 받지 못한다
        KakaoMapView(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = mapBottomInset),
            onMapReady = onMapReady,
            onResumed = onResumed,
        )
        // content 모드(탐색 검색 상세)에선 검색바·칩을 그리지 않는다 (iOS isContentMode 대응)
        if (hideTopBar) return@Box
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MapSearchBar(
                onTap = actions.onOpenSearch,
                query = searchQuery,
                onBack = actions.onReopenSearch,
                onClear = actions.onClearSearch,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            // 검색 결과 모드에선 카테고리 필터 칩을 감춘다 (iOS 동일)
            if (searchQuery == null) {
                CategoryChipBar(selected = selectedCategory, onSelect = actions.onCategorySelected)
            }
        }
    }
}

@Composable
private fun MapSheetContent(
    state: MapState,
    onIntent: (MapIntent) -> Unit,
    height: Dp,
) {
    Column(modifier = Modifier.fillMaxWidth().height(height)) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 8.dp),
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
                        onSelect = { onIntent(MapIntent.OwnershipSelected(OWNERSHIP_ORDER[it])) },
                    )
                }
                FilterDropdown(
                    label = state.selectedCategory?.displayName() ?: CATEGORY_UNFILTERED,
                    isActive = state.selectedCategory != null,
                    options = listOf(CATEGORY_UNFILTERED) + CATEGORY_ORDER.map { it.displayName() },
                    selectedIndex = state.selectedCategory?.let { CATEGORY_ORDER.indexOf(it) + 1 } ?: 0,
                    onSelect = { onIntent(MapIntent.CategorySelected(if (it == 0) null else CATEGORY_ORDER[it - 1])) },
                )
            }
        }
        if (state.loadFailed) {
            MapLoadFailed(
                onRetry = { onIntent(MapIntent.RetryClicked) },
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
        } else if (state.isEmpty) {
            MapEmptyState(
                hasNoSavedPlace = state.hasNoSavedPlace,
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                items(state.filteredPlaces, key = { it.id }) { place ->
                    PlaceListRow(
                        place = place,
                        onEditClick = { onIntent(MapIntent.EditClicked(place.id)) },
                        onDeleteClick = { onIntent(MapIntent.DeleteClicked(place.id)) },
                        onClick = { onIntent(MapIntent.OpenSavedDetail(place)) },
                    )
                }
            }
        }
    }
}

// 검색 결과 모드 시트. 헤더·필터 없이 결과 리스트만 보인다 (iOS searchResultList 대응)
@Composable
private fun SearchResultSheet(
    result: SearchResult,
    state: MapState,
    onIntent: (MapIntent) -> Unit,
    height: Dp,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .padding(top = 8.dp),
    ) {
        items(result.places, key = { it.id }) { place ->
            SearchResultRow(
                place = place,
                isBookmarked = state.isBookmarked(place),
                showsDivider = place.id != result.places.last().id,
                onClick = { onIntent(MapIntent.SearchRowClicked(place)) },
                onBookmark = { onIntent(MapIntent.SearchBookmarkClicked(place)) },
            )
        }
    }
}

// 검색 결과 한 줄: 아이콘 + 이름 + 주소 + 우측 북마크 + 하단 구분선 (iOS PlaceListRow 검색 변형 대응)
@Composable
private fun SearchResultRow(
    place: Place,
    isBookmarked: Boolean,
    showsDivider: Boolean,
    onClick: () -> Unit,
    onBookmark: () -> Unit,
) {
    Column(modifier = Modifier.clickable(onClick = onClick)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(place.category.iconRes()),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = place.name,
                    style = Typography.body1SB,
                    color = Colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = place.roadAddress.ifEmpty { place.address },
                    style = Typography.caption1R,
                    color = Colors.textTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val bookmarkRes = if (isBookmarked) R.drawable.bookmarkfillcolor else R.drawable.bookmarkstroke
            Image(
                painter = painterResource(bookmarkRes),
                contentDescription = if (isBookmarked) "저장 취소" else "저장",
                modifier = Modifier
                    .size(24.dp)
                    .clickable(onClick = onBookmark),
            )
        }
        if (showsDivider) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Colors.borderWeak),
            )
        }
    }
}

// 조회 실패. 빈 목록과 구분해 다시 시도할 길을 준다 (iOS loadState.failed + '다시 시도' 대응)
@Composable
private fun MapLoadFailed(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(top = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "장소를 불러오지 못했어요",
            style = Typography.title3SB,
            color = Colors.textPrimary,
        )
        Spacer(modifier = Modifier.height(16.dp))
        AppButton(
            text = "다시 시도",
            onClick = onRetry,
            variant = AppButtonVariant.OUTLINED,
            size = AppButtonSize.MD,
        )
    }
}

// 저장 장소가 없거나(hasNoSavedPlace) 필터 결과가 비었을 때 문구가 다르다 (iOS EmptyStateView 대응)
@Composable
private fun MapEmptyState(hasNoSavedPlace: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(top = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(painter = painterResource(R.drawable.placeempty), contentDescription = null)
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

// 지도에 찍을 핀 하나. 좌표와 카테고리(아이콘)만 있으면 된다
// 지도에 찍는 핀. id 는 라벨 태그로 심어, 핀을 탭했을 때 어떤 장소인지 찾는다
private data class MapPin(val id: String, val coordinate: Coordinate, val category: PlaceCategory)

// 카테고리 핀을 찍고, selected 좌표가 있으면 그 위에 선택 마커(빨강 물방울)를 얹는다. 카메라는 호출부가 맡는다.
// 카테고리 아이콘은 벡터라 카카오 라벨이 못 그리므로 비트맵으로 래스터화해 스타일로 준다
private fun renderPins(context: Context, map: KakaoMap, pins: List<MapPin>, selected: Coordinate?) {
    val manager = map.labelManager ?: return
    val layer = manager.layer ?: return
    layer.isClickable = true
    layer.removeAll()

    // 카테고리별로 스타일을 한 번만 만들어 재사용한다.
    // SDK 기본 앵커는 하단 중앙(0.5, 1.0)이라 배지가 좌표 위에 뜬다. iOS 처럼 배지 중심을 좌표에 놓는다
    val stylesByCategory = pins.map { it.category }.distinct().associateWith { category ->
        val bitmap = drawableToBitmap(context, category.pinRes())
        manager.addLabelStyles(
            LabelStyles.from(LabelStyle.from(bitmap).setAnchorPoint(0.5f, 0.5f)),
        )
    }

    pins.forEach { pin ->
        val styles = stylesByCategory[pin.category] ?: return@forEach
        layer.addLabel(
            LabelOptions.from(LatLng.from(pin.coordinate.latitude, pin.coordinate.longitude))
                .setStyles(styles)
                // 태그로 어떤 장소의 핀인지 알아낸다. 선택 마커에는 태그를 달지 않아 탭해도 무시된다
                .setTag(pin.id)
                .setClickable(true),
        )
    }

    // 선택 마커는 기존 핀 위에 얹는다. 아래 뾰족한 끝이 좌표를 가리키도록 하단 중앙 앵커,
    // rank 를 배지(기본 0)보다 높여 항상 배지 위에 그려진다 (iOS selected rank 대응)
    if (selected != null) {
        val bitmap = drawableToBitmap(context, R.drawable.map_pin_selected)
        val styles = manager.addLabelStyles(
            LabelStyles.from(LabelStyle.from(bitmap).setAnchorPoint(0.5f, 1.0f)),
        )
        layer.addLabel(
            LabelOptions.from(LatLng.from(selected.latitude, selected.longitude))
                .setStyles(styles)
                .setRank(SELECTED_MARKER_RANK),
        )
    }
}

// 벡터 드로어블을 라벨용 비트맵으로 래스터화한다
private fun drawableToBitmap(context: Context, @DrawableRes resId: Int): Bitmap {
    val drawable = requireNotNull(ContextCompat.getDrawable(context, resId))
    val width = drawable.intrinsicWidth.coerceAtLeast(1)
    val height = drawable.intrinsicHeight.coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    drawable.setBounds(0, 0, width, height)
    drawable.draw(Canvas(bitmap))
    return bitmap
}
