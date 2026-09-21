package com.dulpick.app.domain.place

interface PlaceRepository {
    // 장소 검색 (페이지네이션)
    suspend fun searchPlaces(query: String, page: Int, size: Int): PlacePage

    // 커플이 저장한 장소 전체 (지도·목록)
    suspend fun savedPlaces(): List<SavedPlace>

    // 장소 저장(북마크). 저장된 장소를 돌려준다
    suspend fun savePlace(kakaoPlaceId: String, query: String, alias: String?): SavedPlace

    // 저장 장소 삭제
    suspend fun removePlace(placeId: Long)

    // 별칭 수정. 갱신된 저장 장소를 돌려준다
    suspend fun updateAlias(placeId: Long, alias: String?): SavedPlace
}
