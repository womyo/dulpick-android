package com.dulpick.app.data.notification.remote

import com.dulpick.app.core.network.Authed
import com.dulpick.app.core.network.safeApiCall
import com.dulpick.app.data.notification.remote.dto.PushDeviceRequestDto
import javax.inject.Inject

class PushRemoteDataSource @Inject constructor(
    @Authed private val pushApi: PushApi,
) {
    suspend fun register(deviceId: String, body: PushDeviceRequestDto) =
        safeApiCall { pushApi.register(deviceId, body) }

    suspend fun unregister(deviceId: String) = safeApiCall { pushApi.unregister(deviceId) }
}
