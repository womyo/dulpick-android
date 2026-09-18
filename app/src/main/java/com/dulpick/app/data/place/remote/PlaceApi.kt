package com.dulpick.app.data.place.remote

import com.dulpick.app.data.place.remote.dto.PlaceSearchResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface PlaceApi {
    @GET("/api/v1/places/search")
    suspend fun search(
        @Query("query") query: String,
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): PlaceSearchResponseDto
}
