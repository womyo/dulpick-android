package com.dulpick.app.data.place.remote

import com.dulpick.app.core.network.Authed
import com.dulpick.app.core.network.safeApiCall
import com.dulpick.app.data.place.remote.dto.PlaceAliasRequestDto
import com.dulpick.app.data.place.remote.dto.PlaceSearchResponseDto
import com.dulpick.app.data.place.remote.dto.SavedPlaceResponseDto
import javax.inject.Inject

class PlaceRemoteDataSource @Inject constructor(
    @Authed private val placeApi: PlaceApi,
) {
    suspend fun search(query: String, page: Int, size: Int): PlaceSearchResponseDto =
        safeApiCall { placeApi.search(query, page, size) }

    suspend fun savedPlaces(): List<SavedPlaceResponseDto> =
        safeApiCall { placeApi.savedPlaces() }

    suspend fun removePlace(placeId: Long): Unit =
        safeApiCall { placeApi.removePlace(placeId) }

    suspend fun updateAlias(placeId: Long, alias: String?): SavedPlaceResponseDto =
        safeApiCall { placeApi.updateAlias(placeId, PlaceAliasRequestDto(alias)) }
}
