package com.dulpick.app.data.profile.remote.dto

import kotlinx.serialization.Serializable

// GET /members/me. 온보딩 판정과 닉네임/성향 재사용에 쓴다
@Serializable
data class MemberResponseDto(
    val memberId: Int? = null,
    val onboardingCompleted: Boolean,
    val nickname: String? = null,
    val profileIcon: Int? = null,
    val datePreferences: MemberDatePreferencesResponseDto? = null,
)

@Serializable
data class MemberDatePreferencesResponseDto(
    val indoorOutdoor: String? = null,
    val activityLevel: String? = null,
    val dateTime: String? = null,
    val dateFocus: String? = null,
)
