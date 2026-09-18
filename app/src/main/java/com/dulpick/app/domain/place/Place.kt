package com.dulpick.app.domain.place

// 카테고리 (서버 categoryCode/한글 categoryName 매핑 결과)
enum class PlaceCategory {
    ACCOMMODATION, TOURISM, SHOPPING, ACTIVITY, CONVENIENCE, CAFE, FOOD
}

// 장소. 검색 그리드는 앞쪽 필드만 쓰고, 지도·상세는 좌표·주소·kakaoId 까지 쓴다.
// 뒤쪽은 기본값을 둬 검색 등 기존 생성부를 건드리지 않는다
data class Place(
    val id: String,
    val name: String,
    val category: PlaceCategory,
    val bookmarkCount: Int,
    val thumbnailUrls: List<String>,
    // 저장 API(POST /places) 가 요구하는 Kakao 장소 ID
    val kakaoPlaceId: String? = null,
    val address: String = "",
    val roadAddress: String = "",
    val coordinate: Coordinate = Coordinate(0.0, 0.0),
)

data class PlacePage(
    val items: List<Place>,
    val hasNext: Boolean,
)
