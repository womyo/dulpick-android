package com.dulpick.app.data.couple.remote.dto

import kotlinx.serialization.Serializable

// MARK: - 요청

@Serializable
data class ConnectionCodeRequestDto(
    val connectionCode: String,
)

// MARK: - 응답

@Serializable
data class ConnectionCodeResponseDto(
    val code: String,
    val shareUrl: String? = null,
)

@Serializable
data class CoupleMemberProfileResponseDto(
    val nickname: String,
    val profileIcon: Int,
)

@Serializable
data class CoupleConnectionStatusResponseDto(
    val connected: Boolean,
    val me: CoupleMemberProfileResponseDto? = null,
    val partner: CoupleMemberProfileResponseDto? = null,
    // Domain 이 쓰지 않는 값. 서버가 null 을 주는 필드다
    val connectedAt: String? = null,
    val daysTogether: Int? = null,
)
