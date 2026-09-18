package com.dulpick.app.data.placeimport.remote

import com.dulpick.app.data.placeimport.remote.dto.PlaceImportConfirmRequestDto
import com.dulpick.app.data.placeimport.remote.dto.PlaceImportResponseDto
import com.dulpick.app.data.placeimport.remote.dto.PlaceImportStartRequestDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface PlaceImportApi {
    @POST("/api/v1/place-imports")
    suspend fun start(@Body body: PlaceImportStartRequestDto): PlaceImportResponseDto

    @GET("/api/v1/place-imports/{importId}")
    suspend fun poll(@Path("importId") importId: Long): PlaceImportResponseDto

    @POST("/api/v1/place-imports/{importId}/confirm")
    suspend fun confirm(
        @Path("importId") importId: Long,
        @Body body: PlaceImportConfirmRequestDto,
    )
}
