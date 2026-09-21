package com.dulpick.app.feature.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.dulpick.app.domain.place.PlaceCategory
import com.dulpick.app.domain.place.PlaceOwnership
import com.dulpick.app.feature.map.component.CATEGORY_ORDER
import com.dulpick.app.feature.map.component.CATEGORY_UNFILTERED
import com.dulpick.app.feature.map.component.CategoryChipBar
import com.dulpick.app.feature.map.component.FilterDropdown
import com.dulpick.app.feature.map.component.MapSearchBar
import com.dulpick.app.feature.placedetail.MapDetailArg
import com.dulpick.app.feature.placedetail.PlaceDetailSheet
import com.dulpick.app.feature.map.component.OWNERSHIP_ORDER
import com.dulpick.app.feature.map.component.PlaceAliasSheet
import com.dulpick.app.feature.map.component.PlaceListRow
import com.dulpick.app.feature.map.component.displayName
import com.dulpick.app.ui.component.AppToast
import com.dulpick.app.ui.component.pinRes
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
// 접힘 높이 = 화면 높이의 42% (iOS collapsedScreenRatio 40~45% 범위). 펼침은 거의 전체
private const val SHEET_PEEK_FRACTION = 0.42f
private const val SHEET_EXPANDED_FRACTION = 0.92f
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
    // 검색 결과 탭으로 넘어온 상세 대상(place+query JSON). 검색이 pop 되며 지도로 전달된다
    pendingDetailArg: String? = null,
    onDetailConsumed: () -> Unit = {},
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

    // 검색에서 넘어온 상세 대상(JSON)을 디코드해 지도 상태로 올린다. 검색 장소는 카카오+검색어 조회(serverPlaceId=null)
    LaunchedEffect(pendingDetailArg) {
        val arg = pendingDetailArg?.let(MapDetailArg::decode) ?: return@LaunchedEffect
        viewModel.onIntent(
            MapIntent.OpenDetail(DetailTarget(arg.place.toPlace(), arg.query, serverPlaceId = null)),
        )
        onDetailConsumed()
    }

    // 상세가 열려 있으면 뒤로가기는 상세만 닫는다(지도로 복귀)
    BackHandler(enabled = state.detail != null) { viewModel.onIntent(MapIntent.CloseDetail) }

    Box(modifier = Modifier.fillMaxSize()) {
        BottomSheetScaffold(
            scaffoldState = rememberBottomSheetScaffoldState(),
            sheetPeekHeight = (screenHeight * SHEET_PEEK_FRACTION).dp,
            sheetContainerColor = Colors.commonWhite,
            // 기본 tonalElevation 이 흰색에 톤 오버레이를 얹어 색이 뜨므로 끈다
            sheetTonalElevation = 0.dp,
            sheetShape = RoundedCornerShape(topStart = SHEET_CORNER_RADIUS, topEnd = SHEET_CORNER_RADIUS),
            sheetContent = {
                val detail = state.detail
                if (detail != null) {
                    PlaceDetailSheet(
                        target = detail.place,
                        query = detail.query,
                        serverPlaceId = detail.serverPlaceId,
                        onClose = { viewModel.onIntent(MapIntent.CloseDetail) },
                        onSessionExpired = onSessionExpired,
                        modifier = Modifier.height((screenHeight * SHEET_DETAIL_EXPANDED_FRACTION).dp),
                    )
                } else {
                    MapSheetContent(
                        state = state,
                        onIntent = viewModel::onIntent,
                        height = (screenHeight * SHEET_EXPANDED_FRACTION).dp,
                    )
                }
            },
        ) {
            MapBody(
                selectedCategory = state.selectedCategory,
                onOpenSearch = onOpenSearch,
                onCategorySelected = { viewModel.onIntent(MapIntent.CategorySelected(it)) },
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

    // 상세가 열리면 그 장소만 핀으로 찍고 카메라를 그 자리로 옮긴다(iOS content([place]) 모드).
    // 상세가 없으면 저장 장소 전체를 찍는다. 지도 준비·필터·상세 변경 때마다 다시 그린다
    LaunchedEffect(kakaoMap, state.filteredPlaces, state.detail) {
        renderMap(context, kakaoMap ?: return@LaunchedEffect, state)
    }
}

// 상세 여부에 따라 핀과 카메라를 맞춘다. 상세면 그 장소 하나만, 아니면 저장 장소 전체를 첫 장소로 맞춰 보여준다
private fun renderMap(context: Context, map: KakaoMap, state: MapState) {
    val detail = state.detail
    if (detail != null) {
        val coord = detail.place.coordinate
        renderPins(context, map, listOf(MapPin(coord, detail.place.category)))
        map.moveCamera(
            CameraUpdateFactory.newCenterPosition(LatLng.from(coord.latitude, coord.longitude), DEFAULT_ZOOM_LEVEL),
        )
    } else {
        renderPins(context, map, state.filteredPlaces.map { MapPin(it.place.coordinate, it.place.category) })
        state.filteredPlaces.firstOrNull()?.place?.coordinate?.let { first ->
            map.moveCamera(
                CameraUpdateFactory.newCenterPosition(LatLng.from(first.latitude, first.longitude), DEFAULT_ZOOM_LEVEL),
            )
        }
    }
}

// 지도 + 상단 컨트롤(검색바·카테고리 칩바). 시트 뒤 전체를 채운다
@Composable
private fun MapBody(
    selectedCategory: PlaceCategory?,
    onOpenSearch: () -> Unit,
    onCategorySelected: (PlaceCategory?) -> Unit,
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
            MapSearchBar(onTap = onOpenSearch, modifier = Modifier.padding(horizontal = 20.dp))
            CategoryChipBar(selected = selectedCategory, onSelect = onCategorySelected)
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
