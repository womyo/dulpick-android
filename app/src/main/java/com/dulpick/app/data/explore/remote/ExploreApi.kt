package com.dulpick.app.data.explore.remote

import com.dulpick.app.data.explore.remote.dto.ContentPageResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ExploreApi {
    @GET("/api/v1/contents")
    suspend fun contents(
        @Query("sort") sort: String,
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): ContentPageResponseDto

    // 특정 장소와 관련된 게시물 (장소 상세)
    @GET("/api/v1/places/{placeId}/contents")
    suspend fun placeContents(
        @Path("placeId") placeId: Long,
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
