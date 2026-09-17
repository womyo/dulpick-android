package com.dulpick.app.data.place.mapper

import com.dulpick.app.domain.place.PlaceCategory

// 서버 카테고리 → 도메인 카테고리. 검색·저장장소 등이 함께 쓴다 (iOS PlaceDTOMapper.category 대응)
object PlaceCategoryMapper {
    // 검색/상세 응답은 ASCII categoryCode 우선, 없으면 한글 categoryName 으로 매핑
    fun fromCodeOrName(code: String?, name: String): PlaceCategory =
        when (code) {
            "RESTAURANT" -> PlaceCategory.FOOD
            "CAFE" -> PlaceCategory.CAFE
            "ENTERTAINMENT" -> PlaceCategory.ACTIVITY
            "SHOPPING" -> PlaceCategory.SHOPPING
            "CONVENIENCE" -> PlaceCategory.CONVENIENCE
            "TOURISM" -> PlaceCategory.TOURISM
            "ACCOMMODATION" -> PlaceCategory.ACCOMMODATION
            else -> fromName(name)
        }

    // 저장 목록·저장 응답에는 code 가 없어 한글 categoryName 으로 매핑한다
    fun fromName(name: String): PlaceCategory =
        when (name) {
            "카페" -> PlaceCategory.CAFE
            "관광" -> PlaceCategory.TOURISM
            "놀거리" -> PlaceCategory.ACTIVITY
            "쇼핑" -> PlaceCategory.SHOPPING
            "숙박" -> PlaceCategory.ACCOMMODATION
            "편의", "생활 편의" -> PlaceCategory.CONVENIENCE
            else -> PlaceCategory.FOOD
        }
}
