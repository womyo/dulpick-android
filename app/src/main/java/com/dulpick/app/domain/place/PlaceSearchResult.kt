package com.dulpick.app.domain.place

// 장소 검색 한 건. 아무도 저장하지 않은 장소도 담는다.
// 상세·저장 목록·코스가 쓰는 Place 에 검색에서만 차는 칸을 만들지 않으려고 따로 둔다
data class PlaceSearchResult(
    val place: Place,
    // null 이면 커플 누구도 저장하지 않았다
    val ownership: PlaceOwnership?,
    // 지도 검색 응답에는 별칭이 없어 그쪽 결과는 늘 null 이다
    val alias: String?,
) {
    val id: String get() = place.id

    val isSaved: Boolean get() = ownership != null

    // 커플 누군가 저장한 장소면 저장 목록의 한 줄로 되돌린다. 저장 시각은 검색 응답에 없다
    fun toSavedPlace(): SavedPlace? {
        val ownership = ownership ?: return null
        return SavedPlace(place = place, ownership = ownership, alias = alias, savedAt = null)
    }

    companion object {
        fun of(savedPlace: SavedPlace): PlaceSearchResult = PlaceSearchResult(
            place = savedPlace.place,
            ownership = savedPlace.ownership,
            alias = savedPlace.alias,
        )
    }
}
