package com.dulpick.app.feature.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
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

// 장소 핀. 카테고리 배지를 좌표 한가운데에 놓는다.
// SDK 기본 앵커는 하단 중앙이라 배지가 좌표 위에 떠서 iOS 처럼 가운데로 맞춘다
fun placePin(context: Context, id: String, coordinate: Coordinate, category: PlaceCategory): MapPin =
    MapPin(
        id = id,
        coordinate = coordinate,
        styleId = "category-${category.name}",
        makeStyle = { MapPinStyle(drawableToBitmap(context, category.pinRes()), 0.5f, 0.5f) },
    )

// 선택 마커(빨강 물방울). 아래 뾰족한 끝이 좌표를 가리키도록 하단 중앙 앵커.
// id 가 없어 탭해도 무시된다 — 이미 상세가 열린 장소다
fun selectedPin(context: Context, coordinate: Coordinate): MapPin =
    MapPin(
        id = null,
        coordinate = coordinate,
        styleId = "selected",
        rank = SELECTED_MARKER_RANK,
        makeStyle = { MapPinStyle(drawableToBitmap(context, R.drawable.map_pin_selected), 0.5f, 1.0f) },
    )

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

// 코스에 담을 후보로 고른 장소. 흰 하트가 든 물방울이다 (iOS .candidate 대응).
// 다시 누르면 선택이 풀리도록 id 를 달아 탭을 받는다
fun candidatePin(context: Context, id: String, coordinate: Coordinate): MapPin =
    MapPin(
        id = id,
        coordinate = coordinate,
        styleId = "candidate",
        rank = SELECTED_MARKER_RANK,
        makeStyle = { MapPinStyle(drawableToBitmap(context, R.drawable.map_pin_candidate), 0.5f, 1.0f) },
    )
