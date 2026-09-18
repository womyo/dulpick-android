package com.dulpick.app.data.placeimport.remote

import com.dulpick.app.core.network.Authed
import com.dulpick.app.core.network.safeApiCall
import com.dulpick.app.data.placeimport.remote.dto.PlaceImportConfirmRequestDto
import com.dulpick.app.data.placeimport.remote.dto.PlaceImportResponseDto
import com.dulpick.app.data.placeimport.remote.dto.PlaceImportStartRequestDto
import javax.inject.Inject

class PlaceImportRemoteDataSource @Inject constructor(
    @Authed private val api: PlaceImportApi,
) {
    suspend fun start(body: PlaceImportStartRequestDto): PlaceImportResponseDto =
        safeApiCall { api.start(body) }

    suspend fun poll(importId: Long): PlaceImportResponseDto =
        safeApiCall { api.poll(importId) }

    suspend fun confirm(importId: Long, body: PlaceImportConfirmRequestDto): Unit =
        safeApiCall { api.confirm(importId, body) }
}
