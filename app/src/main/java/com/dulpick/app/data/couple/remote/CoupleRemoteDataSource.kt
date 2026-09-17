package com.dulpick.app.data.couple.remote

import com.dulpick.app.core.network.Authed
import com.dulpick.app.core.network.safeApiCall
import com.dulpick.app.data.couple.remote.dto.ConnectionCodeRequestDto
import com.dulpick.app.data.couple.remote.dto.ConnectionCodeResponseDto
import com.dulpick.app.data.couple.remote.dto.CoupleConnectionStatusResponseDto
import javax.inject.Inject

class CoupleRemoteDataSource @Inject constructor(
    @Authed private val coupleApi: CoupleApi,
) {
    suspend fun connectionCode(): ConnectionCodeResponseDto =
        safeApiCall { coupleApi.connectionCode() }

    suspend fun connect(body: ConnectionCodeRequestDto): CoupleConnectionStatusResponseDto =
        safeApiCall { coupleApi.connect(body) }

    suspend fun current(): CoupleConnectionStatusResponseDto =
        safeApiCall { coupleApi.current() }

    suspend fun disconnect() = safeApiCall { coupleApi.disconnect() }
}
