package com.dulpick.app.data.couple.remote

import com.dulpick.app.data.couple.remote.dto.ConnectionCodeRequestDto
import com.dulpick.app.data.couple.remote.dto.ConnectionCodeResponseDto
import com.dulpick.app.data.couple.remote.dto.CoupleConnectionStatusResponseDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST

interface CoupleApi {
    @GET("/api/v1/connection-codes/me")
    suspend fun connectionCode(): ConnectionCodeResponseDto

    // 커플 연결 끊기
    @DELETE("/api/v1/couples/me")
    suspend fun disconnect()

    @POST("/api/v1/couples")
    suspend fun connect(@Body body: ConnectionCodeRequestDto): CoupleConnectionStatusResponseDto

    @GET("/api/v1/couples/me")
    suspend fun current(): CoupleConnectionStatusResponseDto
}
