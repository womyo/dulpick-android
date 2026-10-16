package com.dulpick.app.data.notification.remote

import com.dulpick.app.data.notification.remote.dto.PushDeviceRequestDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.PUT
import retrofit2.http.Path

interface PushApi {
    // deviceId 는 앱이 만들어 보관한다. 같은 값으로 다시 PUT 하면 덮어쓴다
    @PUT("/api/v1/push-devices/{deviceId}")
    suspend fun register(
        @Path("deviceId") deviceId: String,
        @Body body: PushDeviceRequestDto,
    )

    @DELETE("/api/v1/push-devices/{deviceId}")
    suspend fun unregister(@Path("deviceId") deviceId: String)
}
