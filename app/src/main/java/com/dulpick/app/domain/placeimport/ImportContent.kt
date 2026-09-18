package com.dulpick.app.domain.placeimport

// 공유된 게시물의 메타(제목·캡션·썸네일·작성자) (iOS ImportContent 대응)
data class ImportContent(
    val title: String?,
    val caption: String?,
    val thumbnailUrl: String?,
    val author: ImportAuthor?,
    val publishedOn: String?,
)

data class ImportAuthor(
    val displayName: String,
    val username: String,
)
