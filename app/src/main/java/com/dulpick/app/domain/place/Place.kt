package com.dulpick.app.domain.place

// 카테고리 (서버 categoryCode/한글 categoryName 매핑 결과)
enum class PlaceCategory {
    ACCOMMODATION, TOURISM, SHOPPING, ACTIVITY, CONVENIENCE, CAFE, FOOD
}

// 검색 결과 장소. 리스트가 쓰는 필드만
data class Place(
    val id: String,
    val name: String,
    val category: PlaceCategory,
    val bookmarkCount: Int,
    val thumbnailUrls: List<String>,
)

data class PlacePage(
    val items: List<Place>,
    val hasNext: Boolean,
)
