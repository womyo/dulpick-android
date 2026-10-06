package com.dulpick.app.core.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.route.RouteLineOptions
import com.kakao.vectormap.route.RouteLinePattern
import com.kakao.vectormap.route.RouteLineSegment
import com.kakao.vectormap.route.RouteLineStyle
import com.kakao.vectormap.route.RouteLineStyles
import kotlin.math.ceil

// 선 굵기와 테두리 (iOS routeWidth 4 · routeStrokeWidth 1 대응).
// SDK 는 픽셀을 받는다 — dp 를 그대로 넘기면 밀도만큼 얇아진다
private val ROUTE_WIDTH = 4.dp
private val ROUTE_STROKE_WIDTH = 1.dp
private const val ROUTE_COLOR = 0xFFF6465F.toInt()
private const val ROUTE_STROKE_COLOR = 0xFFFFFFFF.toInt()

// 선을 따라 반복해 찍히는 흰 점. 지름은 선 두께(4)보다 작아 선 안에 들어간다
// (iOS routeDotDiameter 2.2 · routeDotSpacing 35 대응)
private val ROUTE_DOT_DIAMETER = 2.2f.dp
private val ROUTE_DOT_SPACING = 35.dp

// 코스 순서를 잇는 선을 그린다. 점이 둘 미만인 경로는 그릴 게 없다
@Composable
internal fun RenderRoutes(kakaoMap: KakaoMap?, revision: Int, routes: List<MapRoute>) {
    val density = LocalDensity.current
    val widthPx = with(density) { ROUTE_WIDTH.toPx() }
    val strokeWidthPx = with(density) { ROUTE_STROKE_WIDTH.toPx() }
    val dotSpacingPx = with(density) { ROUTE_DOT_SPACING.toPx() }
    val dotDiameterPx = with(density) { ROUTE_DOT_DIAMETER.toPx() }
    // 점 그림은 밀도가 그대로면 한 번만 만든다
    val dot = remember(dotDiameterPx) { routeDotBitmap(dotDiameterPx) }
    LaunchedEffect(kakaoMap, revision, routes, widthPx) {
        val map = kakaoMap ?: return@LaunchedEffect
        val manager = map.routeLineManager ?: return@LaunchedEffect
        val layer = manager.layer ?: return@LaunchedEffect
        layer.removeAll()

        val styles = RouteLineStyles.from(
            RouteLineStyle.from(
                widthPx,
                ROUTE_COLOR,
                strokeWidthPx,
                ROUTE_STROKE_COLOR,
                // 시작·끝에 고정하면 번호 핀과 겹친다
                RouteLinePattern.from(dot, dotSpacingPx),
            ),
        )
        routes.filter { it.coordinates.size > 1 }.forEach { route ->
            val points = route.coordinates.map { LatLng.from(it.latitude, it.longitude) }
            layer.addRouteLine(
                RouteLineOptions.from(route.id, RouteLineSegment.from(points, styles)),
            )
        }
    }
}

// 경로선 위에 찍히는 흰 점 하나. SDK 가 선을 따라 반복해 그린다
private fun routeDotBitmap(diameterPx: Float): Bitmap {
    val size = ceil(diameterPx).toInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val paint = Paint().apply {
        isAntiAlias = true
        color = ROUTE_STROKE_COLOR
        style = Paint.Style.FILL
    }
    Canvas(bitmap).drawCircle(size / 2f, size / 2f, size / 2f, paint)
    return bitmap
}
