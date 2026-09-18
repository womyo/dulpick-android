package com.dulpick.app.data.place.remote

import com.dulpick.app.data.place.remote.dto.PlaceSearchResponseDto
import com.dulpick.app.data.place.remote.dto.SavedPlaceResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface PlaceApi {
    @GET("/api/v1/places/search")
    suspend fun search(
        @Query("query") query: String,
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): PlaceSearchResponseDto

    // 커플이 저장한 장소 전체
    @GET("/api/v1/places")
    suspend fun savedPlaces(): List<SavedPlaceResponseDto>
}
