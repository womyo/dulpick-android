package com.dulpick.app.domain.notice

interface NoticeRepository {
    // 페이지는 0 부터. 크기는 부르는 쪽이 정한다
    suspend fun notices(page: Int, size: Int): NoticePage
}
