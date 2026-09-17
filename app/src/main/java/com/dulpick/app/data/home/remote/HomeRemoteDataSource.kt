package com.dulpick.app.data.home.remote

import com.dulpick.app.core.network.Authed
import com.dulpick.app.core.network.safeApiCall
import com.dulpick.app.data.home.remote.dto.HomeDateCourseDto
import com.dulpick.app.data.home.remote.dto.HomeSummaryResponseDto
import com.dulpick.app.data.home.remote.dto.SavedPlaceItemDto
import javax.inject.Inject

class HomeRemoteDataSource @Inject constructor(
    @Authed private val homeApi: HomeApi,
) {
    suspend fun home(): HomeSummaryResponseDto = safeApiCall { homeApi.home() }

    suspend fun recentSavedPlaces(size: Int): List<SavedPlaceItemDto> =
        safeApiCall { homeApi.recentSavedPlaces(size) }

    suspend fun pastDates(size: Int): List<HomeDateCourseDto> =
        safeApiCall { homeApi.pastDates(size) }
}
