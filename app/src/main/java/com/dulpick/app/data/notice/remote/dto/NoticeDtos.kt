package com.dulpick.app.data.notice.remote.dto

import kotlinx.serialization.Serializable

// 화면이 쓰는 값만 받는다. 나머지 키는 무시된다
@Serializable
data class NoticePageResponseDto(
    val notices: List<NoticeResponseDto> = emptyList(),
    val hasNext: Boolean = false,
)

@Serializable
data class NoticeResponseDto(
    val noticeId: Long,
    val title: String,
    val content: String,
    // ISO-8601. 시각까지 올 수 있어 날짜만 떼어 쓴다
    val createdAt: String? = null,
)
