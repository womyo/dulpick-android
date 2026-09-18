package com.dulpick.app.domain.place

interface PlaceRepository {
    // 장소 검색 (페이지네이션)
    suspend fun searchPlaces(query: String, page: Int, size: Int): PlacePage

    // 커플이 저장한 장소 전체 (지도·목록)
    suspend fun savedPlaces(): List<SavedPlace>
}
