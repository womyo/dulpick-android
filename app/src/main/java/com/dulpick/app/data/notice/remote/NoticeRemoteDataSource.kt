package com.dulpick.app.data.notice.remote

import com.dulpick.app.core.network.safeApiCall
import com.dulpick.app.data.notice.remote.dto.NoticePageResponseDto
import javax.inject.Inject

class NoticeRemoteDataSource @Inject constructor(
    private val api: NoticeApi,
) {
    suspend fun notices(page: Int, size: Int): NoticePageResponseDto =
        safeApiCall { api.notices(page, size) }
}
