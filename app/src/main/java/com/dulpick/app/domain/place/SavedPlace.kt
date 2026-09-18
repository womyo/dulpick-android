package com.dulpick.app.domain.place

// 현재 커플 기준 저장 관계. 서버 ownershipStatus 와 1:1 (iOS PlaceOwnership 대응)
enum class PlaceOwnership { MINE, PARTNER, TOGETHER }

// 저장한 장소 = 장소 + 저장 관계·별칭 (iOS SavedPlace 대응)
data class SavedPlace(
    val place: Place,
    val ownership: PlaceOwnership,
    val alias: String?,
    val savedAt: String?,
) {
    val id: String get() = place.id
}
