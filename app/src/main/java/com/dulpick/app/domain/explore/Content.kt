package com.dulpick.app.domain.explore

// 탐색 그리드에 뿌리는 게시물. 썸네일·제목·포함 장소 수
data class Content(
    val id: String,
    val title: String,
    val placeCount: Int,
    val thumbnailUrls: List<String>,
)

// 게시물 목록 한 페이지. hasNext 로 다음 페이지 유무 판단. popularTags 는 첫 페이지 응답에만 담겨 온다
data class ContentPage(
    val items: List<Content>,
    val hasNext: Boolean,
    val popularTags: List<String>,
)
