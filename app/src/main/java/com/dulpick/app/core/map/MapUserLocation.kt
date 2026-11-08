package com.dulpick.app.core.map

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalDensity
import com.dulpick.app.domain.place.Coordinate
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.label.LabelLayerOptions
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import kotlin.math.roundToInt

// 점과 장소 핀이 겹치면 핀이 위다. 기본 라벨 레이어(10001) 아래에 둔다
private const val USER_LOCATION_Z_ORDER = 5_000
private const val USER_LOCATION_LAYER_ID = "dulpick.map.userLocation"

// 바깥 원 40 · 흰 원 20 · 빨간 원 14 를 겹친다. 세 색 모두 brandPrimary 다
private const val DOT_SIZE_DP = 40f
private const val HALO_ALPHA = 0.15f
private const val SHADOW_DIAMETER_DP = 22f
private const val SHADOW_ALPHA = 0.8f
private const val SHADOW_BLUR_DP = 3.75f
private const val WHITE_DIAMETER_DP = 20f
private const val CORE_DIAMETER_DP = 14f
private const val BRAND_COLOR = 0xFFF6465F.toInt()

@Composable
internal fun RenderUserLocation(kakaoMap: KakaoMap?, revision: Int, userLocation: Coordinate?) {
    val density = LocalDensity.current.density
    LaunchedEffect(kakaoMap, revision, userLocation) {
        val map = kakaoMap ?: return@LaunchedEffect
        val manager = map.labelManager ?: return@LaunchedEffect
        val layer = manager.getLayer(USER_LOCATION_LAYER_ID)
            ?: manager.addLayer(
                LabelLayerOptions.from(USER_LOCATION_LAYER_ID)
                    .setZOrder(USER_LOCATION_Z_ORDER)
                    .setClickable(false),
            )
            ?: return@LaunchedEffect
        layer.removeAll()
        val coordinate = userLocation ?: return@LaunchedEffect
        val styles = manager.addLabelStyles(
            LabelStyles.from(
                LabelStyle.from(userLocationDot(density)).setAnchorPoint(0.5f, 0.5f),
            ),
        ) ?: return@LaunchedEffect
        layer.addLabel(
            LabelOptions
                .from(LatLng.from(coordinate.latitude, coordinate.longitude))
                .setStyles(styles),
        )
    }
}

private fun userLocationDot(density: Float): Bitmap {
    val size = (DOT_SIZE_DP * density).roundToInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val center = size / 2f
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    paint.color = BRAND_COLOR
    paint.alpha = (HALO_ALPHA * 255).roundToInt()
    canvas.drawCircle(center, center, size / 2f, paint)

    // 흰 원의 그림자. 안드로이드 Paint 에는 퍼짐이 없어 한 단계 큰 원을 흐려 아래에 깐다
    paint.alpha = (SHADOW_ALPHA * 255).roundToInt()
    paint.maskFilter = BlurMaskFilter(SHADOW_BLUR_DP * density, BlurMaskFilter.Blur.NORMAL)
    canvas.drawCircle(center, center, SHADOW_DIAMETER_DP * density / 2f, paint)
    paint.maskFilter = null

    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(center, center, WHITE_DIAMETER_DP * density / 2f, paint)

    paint.color = BRAND_COLOR
    canvas.drawCircle(center, center, CORE_DIAMETER_DP * density / 2f, paint)
    return bitmap
}
