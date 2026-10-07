package com.dulpick.app.domain.notice

import java.time.LocalDate

// 공지 한 건. 상세 조회가 없어 목록에서 받은 값을 그대로 상세에 넘긴다 (iOS Notice 대응)
data class Notice(
    val id: String,
    val title: String,
    val content: String,
    // 올린 날. 시각은 쓰지 않는다
    val createdAt: LocalDate?,
)

// 공지 목록 한 페이지 (iOS NoticePage 대응)
data class NoticePage(
    val notices: List<Notice>,
    val hasNext: Boolean,
)
