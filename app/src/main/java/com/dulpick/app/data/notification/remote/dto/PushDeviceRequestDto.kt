package com.dulpick.app.data.notification.remote.dto

import kotlinx.serialization.Serializable

// 푸시 기기 등록 본문. appVersion 이 null 이면 키가 실리지 않는다(explicitNulls = false)
@Serializable
data class PushDeviceRequestDto(
    val platform: String,
    val provider: String,
    val providerRegistrationId: String,
    val appVersion: String? = null,
)
