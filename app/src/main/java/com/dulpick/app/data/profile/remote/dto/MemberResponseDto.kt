package com.dulpick.app.data.profile.remote.dto

import kotlinx.serialization.Serializable

// 로그인 슬라이스에선 온보딩 판정에만 쓴다 (나머지 필드는 무시)
@Serializable
data class MemberResponseDto(
    val memberId: Int? = null,
    val onboardingCompleted: Boolean,
)
