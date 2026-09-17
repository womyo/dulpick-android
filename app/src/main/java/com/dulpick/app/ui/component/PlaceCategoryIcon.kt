package com.dulpick.app.ui.component

import androidx.annotation.DrawableRes
import com.dulpick.app.R
import com.dulpick.app.domain.place.PlaceCategory

// 장소 카테고리 → 아이콘 리소스. 검색 결과·홈 저장장소 등이 함께 쓴다
@DrawableRes
fun PlaceCategory.iconRes(): Int = when (this) {
    PlaceCategory.ACCOMMODATION -> R.drawable.category_accommodation
    PlaceCategory.TOURISM -> R.drawable.category_tourism
    PlaceCategory.SHOPPING -> R.drawable.category_shopping
    PlaceCategory.ACTIVITY -> R.drawable.category_activity
    PlaceCategory.CONVENIENCE -> R.drawable.category_convenience
    PlaceCategory.CAFE -> R.drawable.category_cafe
    PlaceCategory.FOOD -> R.drawable.category_food
}
