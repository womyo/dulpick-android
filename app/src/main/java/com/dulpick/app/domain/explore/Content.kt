package com.dulpick.app.domain.explore

// 탐색 그리드에 뿌리는 게시물. 썸네일·제목·포함 장소 수
data class Content(
    val id: Long,
    val title: String,
    val placeCount: Int,
    val thumbnailUrls: List<String>,
)
