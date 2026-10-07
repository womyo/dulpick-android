package com.dulpick.app.data.notice

import com.dulpick.app.data.notice.mapper.NoticeDtoMapper
import com.dulpick.app.data.notice.mapper.NoticeErrorMapper
import com.dulpick.app.data.notice.remote.NoticeRemoteDataSource
import com.dulpick.app.domain.notice.NoticePage
import com.dulpick.app.domain.notice.NoticeRepository
import javax.inject.Inject

@Suppress("TooGenericExceptionCaught")
class NoticeRepositoryImpl @Inject constructor(
    private val noticeRemote: NoticeRemoteDataSource,
) : NoticeRepository {

    override suspend fun notices(page: Int, size: Int): NoticePage {
        try {
            return NoticeDtoMapper.toPage(noticeRemote.notices(page, size))
        } catch (error: Throwable) {
            throw NoticeErrorMapper.map(error)
        }
    }
}
