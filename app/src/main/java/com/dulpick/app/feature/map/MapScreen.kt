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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

// 지도 탭. 저장 장소를 지도에 핀으로 찍고, 아래 바텀시트에 목록으로 보여준다 (iOS MapView 대응)
private val SEOUL_CITY_HALL = LatLng.from(37.5666, 126.9784)
// iOS multiPlaceZoom. 시작·저장 목록·검색 결과·상세 모두 이 배율을 쓴다(단일 장소도 14)
private const val DEFAULT_ZOOM_LEVEL = 14
// 접힘 높이 = 화면 높이의 42% (iOS collapsedScreenRatio 40~45% 범위).
// 펼침은 화면을 다 덮지 않도록 상단(검색바)을 남긴다
private const val SHEET_PEEK_FRACTION = 0.42f
private const val SHEET_EXPANDED_FRACTION = 0.72f
private val SHEET_CORNER_RADIUS = 32.dp
// 상세 시트는 저장목록보다 낮게 편다. 펼쳐도 검색바 아래에 머물러 지도가 넉넉히 보인다(iOS belowSearchBar)
private const val SHEET_DETAIL_EXPANDED_FRACTION = 0.75f

// 화면에 떠 있는 토스트. isError 면 에러 아이콘을 붙인다
private data class MapToast(val message: String, val isError: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onSessionExpired: () -> Unit,
    onOpenSearch: () -> Unit,
    // 검색 화면에서 되돌아온 검색 결과(query+places JSON). 검색이 pop 되며 지도로 전달된다
    pendingSearchArg: String? = null,
    onSearchConsumed: () -> Unit = {},
    // 검색 결과 모드에서 검색바 뒤로 → 그 검색어로 검색 화면을 다시 연다
    onReopenSearch: (String) -> Unit = {},
    viewModel: MapViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var kakaoMap by remember { mutableStateOf<KakaoMap?>(null) }
    var toast by remember { mutableStateOf<MapToast?>(null) }
    val screenHeight = LocalConfiguration.current.screenHeightDp

    CollectSideEffect(viewModel.sideEffect) { effect ->
        when (effect) {
            MapSideEffect.SessionExpired -> onSessionExpired()
            is MapSideEffect.ShowToast -> toast = MapToast(effect.message, effect.isError)
        }
    }
    LaunchedEffect(Unit) { viewModel.onIntent(MapIntent.OnAppear) }

    // 검색에서 넘어온 결과를 검색 결과 모드로 올린다. selectedIndex 있으면 상세도 연다
    ConsumeSearchArg(pendingSearchArg, onSearchConsumed) { arg ->
        viewModel.onIntent(
            MapIntent.EnterSearchResult(
                searchQuery = arg.searchQuery,
                displayQuery = arg.displayQuery,
                places = arg.places.map { it.toPlace() },
                selectedIndex = arg.selectedIndex,
            ),
        )
    }

    // 뒤로가기: 상세가 열려 있으면 상세만 닫고(검색 결과 리스트로), 검색 결과 모드면 저장 모드로 돌아간다
    BackHandler(enabled = state.detail != null || state.searchResult != null) {
        if (state.detail != null) {
            viewModel.onIntent(MapIntent.CloseDetail)
        } else {
            viewModel.onIntent(MapIntent.ClearSearch)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        BottomSheetScaffold(
            scaffoldState = rememberNonDismissibleScaffoldState(),
            sheetPeekHeight = (screenHeight * SHEET_PEEK_FRACTION).dp,
            sheetContainerColor = Colors.commonWhite,
            // 기본 tonalElevation 이 흰색에 톤 오버레이를 얹어 색이 뜨므로 끈다
            sheetTonalElevation = 0.dp,
            sheetShape = RoundedCornerShape(topStart = SHEET_CORNER_RADIUS, topEnd = SHEET_CORNER_RADIUS),
            sheetContent = {
                MapSheet(
                    state = state,
                    screenHeight = screenHeight,
                    onIntent = viewModel::onIntent,
                    onSessionExpired = onSessionExpired,
                )
            },
        ) {
            MapBody(
                searchQuery = state.searchResult?.displayQuery,
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
                onMapReady = { kakaoMap = it },
            )
        }

        AppToast(
            message = toast?.message,
            onDismiss = { toast = null },
            bottomInset = 16,
            iconRes = if (toast?.isError == true) R.drawable.error else null,
        )
    }

    state.aliasEdit?.let { edit ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { viewModel.onIntent(MapIntent.AliasEditDismissed) },
            sheetState = sheetState,
            containerColor = Colors.commonWhite,
            // 기본 tonalElevation 이 흰색에 톤 오버레이를 얹어 회색으로 뜨므로 끈다
            tonalElevation = 0.dp,
        ) {
            PlaceAliasSheet(
                edit = edit,
                onSave = { viewModel.onIntent(MapIntent.AliasSaveClicked(it)) },
            )
        }
    }

    // 상세/검색결과/저장 순으로 핀과 카메라를 맞춘다. 지도 준비·저장목록·검색결과·상세 변경마다 다시 그린다
    LaunchedEffect(kakaoMap, state.filteredPlaces, state.searchResult, state.detail) {
        renderMap(context, kakaoMap ?: return@LaunchedEffect, state)
    }
}

// 상세면 그 장소 하나, 검색결과면 결과 장소들, 아니면 저장 장소 전체를 찍고 첫 장소로 카메라를 맞춘다
private fun renderMap(context: Context, map: KakaoMap, state: MapState) {
    val detail = state.detail
    val searchResult = state.searchResult
    val pins = when {
        detail != null -> listOf(MapPin(detail.place.coordinate, detail.place.category))
        searchResult != null -> searchResult.places.map { MapPin(it.coordinate, it.category) }
        else -> state.filteredPlaces.map { MapPin(it.place.coordinate, it.place.category) }
    }
    renderPins(context, map, pins)
    pins.firstOrNull()?.coordinate?.let { first ->
        map.moveCamera(
            CameraUpdateFactory.newCenterPosition(LatLng.from(first.latitude, first.longitude), DEFAULT_ZOOM_LEVEL),
        )
    }
}

// 아래로 드래그해도 닫히지 않는 시트 상태. skipHiddenState=true 라 접힘/펼침 두 단계만 오간다
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun rememberNonDismissibleScaffoldState() = rememberBottomSheetScaffoldState(
    bottomSheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded,
        skipHiddenState = true,
    ),
)

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
    screenHeight: Int,
    onIntent: (MapIntent) -> Unit,
    onSessionExpired: () -> Unit,
) {
    val detail = state.detail
    val searchResult = state.searchResult
    when {
        detail != null -> PlaceDetailSheet(
            target = detail.place,
            query = detail.query,
            serverPlaceId = detail.serverPlaceId,
            onClose = { onIntent(MapIntent.CloseDetail) },
            onSessionExpired = onSessionExpired,
            modifier = Modifier.height((screenHeight * SHEET_DETAIL_EXPANDED_FRACTION).dp),
        )
        searchResult != null -> SearchResultSheet(
            result = searchResult,
            state = state,
            onIntent = onIntent,
            height = (screenHeight * SHEET_DETAIL_EXPANDED_FRACTION).dp,
        )
        else -> MapSheetContent(
            state = state,
            onIntent = onIntent,
            height = (screenHeight * SHEET_EXPANDED_FRACTION).dp,
        )
    }
}

// 지도 + 상단 컨트롤(검색바·카테고리 칩바). 시트 뒤 전체를 채운다.
// 검색 결과 모드(searchQuery != null)면 검색바가 [뒤로][검색어 X]로 바뀌고 카테고리 칩은 감춘다
@Composable
private fun MapBody(
    searchQuery: String?,
    selectedCategory: PlaceCategory?,
    actions: MapTopBarActions,
    onMapReady: (KakaoMap) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        KakaoMapView(
            modifier = Modifier.fillMaxSize(),
            onMapReady = { map ->
                map.moveCamera(CameraUpdateFactory.newCenterPosition(SEOUL_CITY_HALL, DEFAULT_ZOOM_LEVEL))
                onMapReady(map)
            },
        )
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
        if (state.isEmpty) {
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
private data class MapPin(val coordinate: Coordinate, val category: PlaceCategory)

// 핀 목록을 카테고리 아이콘으로 찍는다. 카메라 이동은 호출부가 맡는다.
// 카테고리 아이콘은 벡터라 카카오 라벨이 못 그리므로 비트맵으로 래스터화해 스타일로 준다
private fun renderPins(context: Context, map: KakaoMap, pins: List<MapPin>) {
    val manager = map.labelManager ?: return
    val layer = manager.layer ?: return
    layer.removeAll()

    // 카테고리별로 스타일을 한 번만 만들어 재사용한다
    val stylesByCategory = pins.map { it.category }.distinct().associateWith { category ->
        val bitmap = drawableToBitmap(context, category.pinRes())
        manager.addLabelStyles(LabelStyles.from(LabelStyle.from(bitmap)))
    }

    pins.forEach { pin ->
        val styles = stylesByCategory[pin.category] ?: return@forEach
        layer.addLabel(
            LabelOptions.from(LatLng.from(pin.coordinate.latitude, pin.coordinate.longitude)).setStyles(styles),
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
