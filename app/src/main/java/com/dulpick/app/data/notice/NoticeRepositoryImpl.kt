package com.dulpick.app.data.notice

import com.dulpick.app.data.notice.mapper.NoticeDtoMapper
import com.dulpick.app.data.notice.remote.NoticeRemoteDataSource
import com.dulpick.app.domain.notice.NoticePage
import com.dulpick.app.domain.notice.NoticeRepository
import javax.inject.Inject

class NoticeRepositoryImpl @Inject constructor(
    private val noticeRemote: NoticeRemoteDataSource,
) : NoticeRepository {

    // 공지는 실패를 구분해 쓸 데가 없다. 화면이 빈 목록으로 받아 "없어요" 를 띄운다
    override suspend fun notices(page: Int, size: Int): NoticePage =
        NoticeDtoMapper.toPage(noticeRemote.notices(page, size))
}
