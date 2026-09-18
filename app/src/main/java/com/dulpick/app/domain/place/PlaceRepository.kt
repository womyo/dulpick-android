package com.dulpick.app.domain.place

interface PlaceRepository {
    // 장소 검색 (페이지네이션)
    suspend fun searchPlaces(query: String, page: Int, size: Int): PlacePage
}
