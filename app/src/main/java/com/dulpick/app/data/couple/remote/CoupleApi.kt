package com.dulpick.app.data.couple.remote

import com.dulpick.app.data.couple.remote.dto.ConnectionCodeRequestDto
import com.dulpick.app.data.couple.remote.dto.ConnectionCodeResponseDto
import com.dulpick.app.data.couple.remote.dto.CoupleConnectionStatusResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface CoupleApi {
    @GET("/api/v1/connection-codes/me")
    suspend fun connectionCode(): ConnectionCodeResponseDto

    @POST("/api/v1/couples")
    suspend fun connect(@Body body: ConnectionCodeRequestDto): CoupleConnectionStatusResponseDto

    @GET("/api/v1/couples/me")
    suspend fun current(): CoupleConnectionStatusResponseDto
}
