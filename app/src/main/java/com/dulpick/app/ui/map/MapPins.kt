package com.dulpick.app.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Typeface
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.dulpick.app.R
import com.dulpick.app.core.map.MapPin
import com.dulpick.app.core.map.MapPinStyle
import com.dulpick.app.domain.place.Coordinate
import com.dulpick.app.domain.place.PlaceCategory
import com.dulpick.app.ui.component.pinRes

// 선택 마커가 카테고리 배지(기본 rank 0) 위에 항상 오도록 하는 rank (iOS selected rank 1 대응)
private const val SELECTED_MARKER_RANK = 1L

// 물방울 머리 중심의 세로 비율과, 머리에 들어가는 내용 크기 비율
private const val HEAD_CENTER_RATIO = 0.40f
private const val DOT_RADIUS_RATIO = 0.16f
private const val HEART_RATIO = 0.40f
private const val NUMBER_TEXT_RATIO = 0.40f

// 장소 핀. 카테고리 배지를 좌표 한가운데에 놓는다.
// SDK 기본 앵커는 하단 중앙이라 배지가 좌표 위에 떠서 iOS 처럼 가운데로 맞춘다
fun placePin(context: Context, id: String, coordinate: Coordinate, category: PlaceCategory): MapPin =
    MapPin(
        id = id,
        coordinate = coordinate,
        styleId = "category-${category.name}",
        makeStyle = { MapPinStyle(drawableToBitmap(context, category.pinRes()), 0.5f, 0.5f) },
    )

// 물방울 머리에 얹는 내용. iOS MapPlacePinContent 와 같다
private sealed interface Droplet {
    data object Selected : Droplet
    data object Candidate : Droplet
    data class Numbered(val number: Int) : Droplet
}

// 선택 마커. 머리 가운데에 흰 원이다. 아래 뾰족한 끝이 좌표를 가리킨다.
// id 가 없어 탭해도 무시된다 — 이미 상세가 열린 장소다
fun selectedPin(context: Context, coordinate: Coordinate): MapPin =
    dropletPin(context, null, coordinate, "selected", Droplet.Selected)

// 코스에 담을 후보로 고른 장소. 흰 하트가 든 물방울이다 (iOS .candidate 대응).
// 다시 누르면 선택이 풀리도록 id 를 달아 탭을 받는다
fun candidatePin(context: Context, id: String, coordinate: Coordinate): MapPin =
    dropletPin(context, id, coordinate, "candidate", Droplet.Candidate)

// 코스 순서 번호 핀 ①②③ (iOS .numbered 대응).
// 같은 장소에 카테고리 핀과 겹쳐 찍히므로 rank 를 높여 위에 둔다
fun numberedPin(context: Context, id: String, coordinate: Coordinate, number: Int): MapPin =
    dropletPin(context, id, coordinate, "numbered-$number", Droplet.Numbered(number))

private fun dropletPin(
    context: Context,
    id: String?,
    coordinate: Coordinate,
    styleId: String,
    content: Droplet,
): MapPin = MapPin(
    id = id,
    coordinate = coordinate,
    styleId = styleId,
    rank = SELECTED_MARKER_RANK,
    makeStyle = { MapPinStyle(dropletBitmap(context, content), 0.5f, 1.0f) },
)

// 물방울 하나를 깔고 머리에 내용을 그린다.
// iOS 는 SwiftUI 뷰를 그림으로 구워 쓰는데, 안드로이드엔 그런 길이 없어 직접 그린다
private fun dropletBitmap(context: Context, content: Droplet): Bitmap {
    val base = drawableToBitmap(context, R.drawable.map_pin_droplet)
    val bitmap = Bitmap.createBitmap(base.width, base.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawBitmap(base, 0f, 0f, null)
    // 물방울 머리 중심
    val centerX = base.width / 2f
    val centerY = base.height * HEAD_CENTER_RATIO
    when (content) {
        Droplet.Selected -> canvas.drawCircle(
            centerX,
            centerY,
            base.width * DOT_RADIUS_RATIO,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE },
        )
        Droplet.Candidate -> canvas.drawHeart(context, centerX, centerY, base.width * HEART_RATIO)
        is Droplet.Numbered -> canvas.drawNumber(content.number, centerX, centerY, base.height)
    }
    return bitmap
}

private fun Canvas.drawHeart(context: Context, centerX: Float, centerY: Float, side: Float) {
    val heart = drawableToBitmap(context, R.drawable.heart)
    val scaled = Bitmap.createScaledBitmap(heart, side.toInt(), side.toInt(), true)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        // 하트 에셋은 빨강이라 흰색으로 덮어 그린다
        colorFilter = PorterDuffColorFilter(android.graphics.Color.WHITE, PorterDuff.Mode.SRC_IN)
    }
    drawBitmap(scaled, centerX - side / 2, centerY - side / 2, paint)
}

private fun Canvas.drawNumber(number: Int, centerX: Float, centerY: Float, pinHeight: Int) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = pinHeight * NUMBER_TEXT_RATIO
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    drawText(number.toString(), centerX, centerY - (paint.descent() + paint.ascent()) / 2, paint)
}

// 벡터 드로어블을 라벨용 비트맵으로 래스터화한다. 카카오 라벨은 벡터를 못 그린다
private fun drawableToBitmap(context: Context, @DrawableRes resId: Int): Bitmap {
    val drawable = requireNotNull(ContextCompat.getDrawable(context, resId))
    val width = drawable.intrinsicWidth.coerceAtLeast(1)
    val height = drawable.intrinsicHeight.coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    drawable.setBounds(0, 0, width, height)
    drawable.draw(Canvas(bitmap))
    return bitmap
}
