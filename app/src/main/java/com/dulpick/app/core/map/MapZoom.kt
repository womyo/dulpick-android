package com.dulpick.app.core.map

import com.dulpick.app.domain.place.Coordinate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.tan

// 코스 결과 화면이 쓰는 줌 계산.
//
// 지도 SDK 없이 도는 순수 계산이다.
// 카카오 지도의 zoomLevel 은 웹 메르카토르 z 와 같은 뜻이다 — 한 단계 오르면 축척이 두 배다.
// (iOS MapZoom 대응)
object MapZoom {

    // 더 줌아웃하지 않는 바닥. 여기 닿으면 일부 장소가 화면 밖에 남는다
    const val LOWER_BOUND = 6

    // 카메라가 시트 윗면 대비 초점을 두는 비율. 지도 쪽과 같은 값을 써야 계산이 맞는다
    const val MAP_FOCUS_RATIO = 0.65f

    // 지도 타일 한 장의 크기 (dp)
    private const val TILE_SIZE = 256.0

    // anchor 를 focusRatio 자리에 둔 채, 모든 좌표가 보이는 영역에 들어오는 가장 큰 줌.
    //
    // focusRatio 0.5 는 한가운데다. 남북은 초점 위·아래 여유를 따로 잰다.
    // 코스 한가운데를 중심으로 잡는 것보다 더 많이 줌아웃한다.
    //
    // viewWidth 는 지도 뷰의 가로 dp, visibleHeight 는 시트 위에 보이는 세로 dp 다.
    // 한 줌 단계가 덮는 땅은 화면 밀도와 무관하므로 픽셀을 넣으면 그만큼 더 당겨진다.
    // maximum 보다 더 당기지 않고 minimum 보다 더 밀지 않는다
    @Suppress("LongParameterList", "ReturnCount")
    fun fit(
        coordinates: List<Coordinate>,
        anchor: Coordinate,
        viewWidth: Float,
        visibleHeight: Float,
        maximum: Int,
        minimum: Int = LOWER_BOUND,
        focusRatio: Float = 0.5f,
    ): Int {
        if (maximum < minimum) return maximum
        if (viewWidth <= 0f || visibleHeight <= 0f) return maximum
        if (focusRatio <= 0f || focusRatio >= 1f) return maximum

        val span = span(coordinates, anchor, focusRatio) ?: return maximum
        for (level in maximum downTo minimum) {
            val worldPixels = TILE_SIZE * 2.0.pow(level)
            val fitsWidth = span.x * worldPixels <= viewWidth
            val fitsHeight = span.y * worldPixels <= visibleHeight
            if (fitsWidth && fitsHeight) return level
        }
        return minimum
    }

    // anchor 를 초점에 둘 때 담아야 하는 가로·세로 폭. 정규 좌표 1.0 이 세계 한 바퀴다.
    // 좌표가 anchor 하나뿐이면 담을 폭이 없어 null 이다
    private fun span(coordinates: List<Coordinate>, anchor: Coordinate, focusRatio: Float): Span? {
        var west = 0.0
        var east = 0.0
        var north = 0.0
        var south = 0.0
        val anchorPoint = normalized(anchor)

        coordinates.forEach { coordinate ->
            val point = normalized(coordinate)
            val dx = point.first - anchorPoint.first
            val dy = point.second - anchorPoint.second
            if (dx >= 0) east = max(east, dx) else west = max(west, -dx)
            if (dy >= 0) south = max(south, dy) else north = max(north, -dy)
        }

        // anchor 하나뿐이면 담을 폭이 없다
        if (maxOf(west, east, north, south) <= 0) return null
        return Span(
            x = max(west, east) * 2,
            y = max(north / focusRatio, south / (1 - focusRatio)),
        )
    }

    private data class Span(val x: Double, val y: Double)

    // 웹 메르카토르 정규 좌표. 둘 다 0...1 이다
    private fun normalized(coordinate: Coordinate): Pair<Double, Double> {
        val x = (coordinate.longitude + 180) / 360
        val clampedLatitude = coordinate.latitude.coerceIn(-MERCATOR_LATITUDE_LIMIT, MERCATOR_LATITUDE_LIMIT)
        val radians = clampedLatitude * PI / 180
        val y = (1 - ln(tan(radians) + 1 / cos(radians)) / PI) / 2
        return x to y
    }

    private const val MERCATOR_LATITUDE_LIMIT = 85.05112878
}
