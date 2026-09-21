package com.dulpick.app.feature.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
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
import com.dulpick.app.domain.place.PlaceOwnership
import com.dulpick.app.domain.place.SavedPlace
import com.dulpick.app.feature.map.component.CATEGORY_ORDER
import com.dulpick.app.feature.map.component.CATEGORY_UNFILTERED
import com.dulpick.app.feature.map.component.CategoryChipBar
import com.dulpick.app.feature.map.component.FilterDropdown
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
private const val DEFAULT_ZOOM_LEVEL = 15
// 접힘 높이 = 화면 높이의 45% (iOS collapsedScreenRatio 0.45). 펼침은 거의 전체
private const val SHEET_PEEK_FRACTION = 0.45f
private const val SHEET_EXPANDED_FRACTION = 0.92f
private val SHEET_CORNER_RADIUS = 32.dp

// 화면에 떠 있는 토스트. isError 면 에러 아이콘을 붙인다
private data class MapToast(val message: String, val isError: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onSessionExpired: () -> Unit,
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

    Box(modifier = Modifier.fillMaxSize()) {
        BottomSheetScaffold(
            scaffoldState = rememberBottomSheetScaffoldState(),
            sheetPeekHeight = (screenHeight * SHEET_PEEK_FRACTION).dp,
            sheetContainerColor = Colors.commonWhite,
            // 기본 tonalElevation 이 흰색에 톤 오버레이를 얹어 색이 뜨므로 끈다
            sheetTonalElevation = 0.dp,
            sheetShape = RoundedCornerShape(topStart = SHEET_CORNER_RADIUS, topEnd = SHEET_CORNER_RADIUS),
            sheetContent = {
                MapSheetContent(
                    state = state,
                    onIntent = viewModel::onIntent,
                    height = (screenHeight * SHEET_EXPANDED_FRACTION).dp,
                )
            },
        ) {
            // 지도는 시트 뒤 전체를 채운다(시트 인셋 무시). 딤 없이 지도가 그대로 조작된다
            Box(modifier = Modifier.fillMaxSize()) {
                KakaoMapView(
                    modifier = Modifier.fillMaxSize(),
                    onMapReady = { map ->
                        map.moveCamera(CameraUpdateFactory.newCenterPosition(SEOUL_CITY_HALL, DEFAULT_ZOOM_LEVEL))
                        kakaoMap = map
                    },
                )
                // 지도 위에 떠 있는 카테고리 칩바
                CategoryChipBar(
                    selected = state.selectedCategory,
                    onSelect = { viewModel.onIntent(MapIntent.CategorySelected(it)) },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(top = 8.dp),
                )
            }
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

    // 지도 준비/필터 변경 시 라벨 레이어를 다시 그린다
    LaunchedEffect(kakaoMap, state.filteredPlaces) {
        renderPlacePins(context, kakaoMap ?: return@LaunchedEffect, state.filteredPlaces)
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
                        onClick = {},
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

// 저장 장소를 카테고리 아이콘 핀으로 찍는다. 목록이 있으면 카메라를 첫 장소로 옮겨 바로 보이게 한다.
// 카테고리 아이콘은 벡터라 카카오 라벨이 못 그리므로 비트맵으로 래스터화해 스타일로 준다
private fun renderPlacePins(context: Context, map: KakaoMap, places: List<SavedPlace>) {
    val manager = map.labelManager ?: return
    val layer = manager.layer ?: return
    layer.removeAll()

    // 카테고리별로 스타일을 한 번만 만들어 재사용한다
    val stylesByCategory = places.map { it.place.category }.distinct().associateWith { category ->
        val bitmap = drawableToBitmap(context, category.pinRes())
        manager.addLabelStyles(LabelStyles.from(LabelStyle.from(bitmap)))
    }

    places.forEach { saved ->
        val coordinate = saved.place.coordinate
        val styles = stylesByCategory[saved.place.category] ?: return@forEach
        layer.addLabel(
            LabelOptions.from(LatLng.from(coordinate.latitude, coordinate.longitude)).setStyles(styles),
        )
    }

    places.firstOrNull()?.place?.coordinate?.let { first ->
        map.moveCamera(
            CameraUpdateFactory.newCenterPosition(LatLng.from(first.latitude, first.longitude), DEFAULT_ZOOM_LEVEL),
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
