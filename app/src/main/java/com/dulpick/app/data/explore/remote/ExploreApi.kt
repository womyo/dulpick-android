package com.dulpick.app.data.explore.remote

import com.dulpick.app.data.explore.remote.dto.ContentPageResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface ExploreApi {
    @GET("/api/v1/contents")
    suspend fun contents(
        @Query("sort") sort: String,
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): ContentPageResponseDto

    @GET("/api/v1/contents/search")
    suspend fun search(
        @Query("query") query: String,
        @Query("sort") sort: String,
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): ContentPageResponseDto
}
