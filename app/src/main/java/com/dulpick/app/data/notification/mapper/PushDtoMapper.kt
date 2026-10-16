package com.dulpick.app.data.notification.mapper

import com.dulpick.app.BuildConfig
import com.dulpick.app.data.notification.remote.dto.PushDeviceRequestDto

object PushDtoMapper {
    private const val PLATFORM = "ANDROID"
    private const val PROVIDER = "FCM"

    fun toRequest(token: String): PushDeviceRequestDto = PushDeviceRequestDto(
        platform = PLATFORM,
        provider = PROVIDER,
        providerRegistrationId = token,
        appVersion = BuildConfig.VERSION_NAME,
    )
}
