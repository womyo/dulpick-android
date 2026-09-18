package com.dulpick.app.feature.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dulpick.app.core.map.KakaoMapView
import com.dulpick.app.core.mvi.CollectSideEffect
import com.dulpick.app.domain.place.SavedPlace
import com.dulpick.app.ui.component.pinRes
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles

// 지도 탭. 단계 1: 저장 장소를 불러와 지도에 카테고리 아이콘 핀으로 찍는다 (iOS MapView 대응)
private val SEOUL_CITY_HALL = LatLng.from(37.5666, 126.9784)
private const val DEFAULT_ZOOM_LEVEL = 15

@Composable
fun MapScreen(
    onSessionExpired: () -> Unit,
    viewModel: MapViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var kakaoMap by remember { mutableStateOf<KakaoMap?>(null) }

    CollectSideEffect(viewModel.sideEffect) { effect ->
        when (effect) {
            MapSideEffect.SessionExpired -> onSessionExpired()
        }
    }
    LaunchedEffect(Unit) { viewModel.onIntent(MapIntent.OnAppear) }

    KakaoMapView(
        modifier = Modifier.fillMaxSize(),
        onMapReady = { map ->
            map.moveCamera(CameraUpdateFactory.newCenterPosition(SEOUL_CITY_HALL, DEFAULT_ZOOM_LEVEL))
            kakaoMap = map
        },
    )

    // 지도 준비/저장장소 변경 시 라벨 레이어를 다시 그린다
    LaunchedEffect(kakaoMap, state.places) {
        renderPlacePins(context, kakaoMap ?: return@LaunchedEffect, state.places)
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
