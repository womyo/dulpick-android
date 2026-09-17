package com.dulpick.app.data.place.remote

import com.dulpick.app.core.network.Authed
import com.dulpick.app.core.network.safeApiCall
import com.dulpick.app.data.place.remote.dto.PlaceSearchResponseDto
import javax.inject.Inject

class PlaceRemoteDataSource @Inject constructor(
    @Authed private val placeApi: PlaceApi,
) {
    suspend fun search(query: String, page: Int, size: Int): PlaceSearchResponseDto =
        safeApiCall { placeApi.search(query, page, size) }
}
