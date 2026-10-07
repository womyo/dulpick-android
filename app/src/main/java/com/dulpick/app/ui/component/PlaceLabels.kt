package com.dulpick.app.ui.component

import com.dulpick.app.domain.place.PlaceCategory
import com.dulpick.app.domain.place.PlaceOwnership

// 소유자 드롭다운 노출 순서. 함께 저장한 이 맨 앞 (iOS mapDisplayOrder 대응)
val OWNERSHIP_ORDER = listOf(PlaceOwnership.TOGETHER, PlaceOwnership.MINE, PlaceOwnership.PARTNER)

fun PlaceOwnership.displayName(): String = when (this) {
    PlaceOwnership.TOGETHER -> "함께 저장한"
    PlaceOwnership.MINE -> "내가 저장한"
    PlaceOwnership.PARTNER -> "상대가 저장한"
}

// 지도 칩·시트 드롭다운이 함께 쓰는 카테고리 순서 (iOS mapDisplayOrder 대응)
val CATEGORY_ORDER = listOf(
    PlaceCategory.FOOD,
    PlaceCategory.CAFE,
    PlaceCategory.ACTIVITY,
    PlaceCategory.SHOPPING,
    PlaceCategory.CONVENIENCE,
    PlaceCategory.TOURISM,
    PlaceCategory.ACCOMMODATION,
)

// 카테고리 드롭다운에서 전체를 뜻하는 문구
const val CATEGORY_UNFILTERED = "전체"

fun PlaceCategory.displayName(): String = when (this) {
    PlaceCategory.FOOD -> "맛집"
    PlaceCategory.CAFE -> "카페"
    PlaceCategory.ACTIVITY -> "놀거리"
    PlaceCategory.SHOPPING -> "쇼핑"
    PlaceCategory.ACCOMMODATION -> "숙박"
    PlaceCategory.TOURISM -> "관광"
    PlaceCategory.CONVENIENCE -> "생활편의"
}
