package com.dulpick.app.domain.place

// 현재 커플 기준 저장 관계. 서버 ownershipStatus 와 1:1 (iOS PlaceOwnership 대응)
enum class PlaceOwnership { MINE, PARTNER, TOGETHER }

// 이 값을 필터로 썼을 때 해당 저장 관계의 장소를 담는지.
// together 는 전체, mine/partner 는 함께 저장(together)한 것도 포함한다 (iOS matches 대응)
fun PlaceOwnership.matches(ownership: PlaceOwnership): Boolean = when (this) {
    PlaceOwnership.TOGETHER -> true
    PlaceOwnership.MINE -> ownership == PlaceOwnership.MINE || ownership == PlaceOwnership.TOGETHER
    PlaceOwnership.PARTNER -> ownership == PlaceOwnership.PARTNER || ownership == PlaceOwnership.TOGETHER
}

// 저장한 장소 = 장소 + 저장 관계·별칭 (iOS SavedPlace 대응)
data class SavedPlace(
    val place: Place,
    val ownership: PlaceOwnership,
    val alias: String?,
    val savedAt: String?,
) {
    val id: String get() = place.id
}
