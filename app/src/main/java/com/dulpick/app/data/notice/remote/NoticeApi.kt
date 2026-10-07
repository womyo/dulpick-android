package com.dulpick.app.data.notice.remote

import com.dulpick.app.data.notice.remote.dto.NoticePageResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface NoticeApi {
    // 로그인 없이도 읽는다 (iOS 가 plainClient 로 부르는 것과 같다)
    @GET("/api/v1/notices")
    suspend fun notices(
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): NoticePageResponseDto
}
