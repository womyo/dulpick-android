package com.dulpick.app.data.notice.mapper

import com.dulpick.app.data.notice.remote.dto.NoticePageResponseDto
import com.dulpick.app.data.notice.remote.dto.NoticeResponseDto
import com.dulpick.app.domain.notice.Notice
import com.dulpick.app.domain.notice.NoticePage
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime

object NoticeDtoMapper {
    fun toPage(dto: NoticePageResponseDto): NoticePage = NoticePage(
        notices = dto.notices.map(::toNotice),
        hasNext = dto.hasNext,
    )

    private fun toNotice(dto: NoticeResponseDto): Notice = Notice(
        id = dto.noticeId.toString(),
        title = dto.title,
        content = dto.content,
        createdAt = dto.createdAt?.let(::toDate),
    )

    // 시각·시간대가 붙어 와도 날짜만 쓴다. 못 읽으면 null 이라 화면이 날짜 줄을 비운다
    private fun toDate(raw: String): LocalDate? =
        runCatching { OffsetDateTime.parse(raw).toLocalDate() }.getOrNull()
            ?: runCatching { LocalDateTime.parse(raw).toLocalDate() }.getOrNull()
            ?: runCatching { LocalDate.parse(raw) }.getOrNull()
}
