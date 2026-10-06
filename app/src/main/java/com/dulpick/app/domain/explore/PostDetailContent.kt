package com.dulpick.app.domain.explore

import com.dulpick.app.domain.place.Place

// 게시글 상세가 그리는 값 (iOS PostDetailContent 대응)
data class PostDetailContent(
    val id: String,
    val title: String?,
    // 본문 또는 캡션
    val caption: String?,
    // 추적 파라미터를 없앤 인스타그램 링크
    val canonicalUrl: String?,
    val places: List<ContentPlace>,
)

// 게시글에 딸린 장소 + 진입 시점의 저장 여부 (iOS ContentPlace 대응)
data class ContentPlace(
    val place: Place,
    val isSaved: Boolean,
) {
    val id: String get() = place.id
}
