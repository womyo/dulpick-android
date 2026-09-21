package com.dulpick.app.domain.place

// 장소 상세 조회 응답. SavedPlace 처럼 Place 를 품고 상세 전용 값을 곁들인다 (iOS PlaceDetail 대응).
// 별칭은 여기 없다 — 상세 응답이 별칭을 주지 않는다
data class PlaceDetail(
    val place: Place,
    // 내가 저장했는지 (응답 savedByMe)
    val savedByMe: Boolean,
    // 화면의 "저장한 사람 N" (응답 savedMemberCount)
    val savedMemberCount: Int,
    // 저장 관계. 아무도 저장하지 않았으면 null
    val ownership: PlaceOwnership?,
    // 지금 화면은 안 쓰지만 응답에 있어 담아둔다
    val phone: String?,
    // 카카오맵 앱이 없을 때 여는 웹 주소
    val kakaoPlaceUrl: String?,
) {
    val id: String get() = place.id
}
