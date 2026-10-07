package com.dulpick.app.feature.notice

import com.dulpick.app.domain.notice.Notice
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate

// 공지 상세는 조회 API 가 없어 목록에서 받은 값을 통째로 넘긴다.
// domain 모델에 직렬화 어노테이션을 붙이지 않으려고 화면 계층의 타입으로 감싼다
@Serializable
data class NoticeArg(
    val id: String,
    val title: String,
    val content: String,
    // yyyy-MM-dd. 날짜를 못 읽었으면 비어 있다
    val createdAt: String? = null,
) {
    fun encode(): String = Json.encodeToString(this)

    fun toNotice(): Notice = Notice(
        id = id,
        title = title,
        content = content,
        createdAt = createdAt?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
    )

    companion object {
        fun from(notice: Notice): NoticeArg = NoticeArg(
            id = notice.id,
            title = notice.title,
            content = notice.content,
            createdAt = notice.createdAt?.toString(),
        )

        fun decode(raw: String): NoticeArg? =
            runCatching { Json.decodeFromString<NoticeArg>(raw) }.getOrNull()
    }
}

// "26.08.05". 날짜가 없으면 빈 글자
internal fun LocalDate?.shortDateText(): String =
    this?.let { "%02d.%02d.%02d".format(it.year % 100, it.monthValue, it.dayOfMonth) } ?: ""
