package com.dulpick.app.data.home.remote

import com.dulpick.app.data.home.remote.dto.HomeDateCourseDto
import com.dulpick.app.data.home.remote.dto.HomeSummaryResponseDto
import com.dulpick.app.data.home.remote.dto.SavedPlaceItemDto
import retrofit2.http.GET
import retrofit2.http.Query

interface HomeApi {
    @GET("/api/v1/home")
    suspend fun home(): HomeSummaryResponseDto

    @GET("/api/v1/home/recent-saved-places")
    suspend fun recentSavedPlaces(@Query("size") size: Int): List<SavedPlaceItemDto>

    @GET("/api/v1/home/past-dates")
    suspend fun pastDates(@Query("size") size: Int): List<HomeDateCourseDto>
}
