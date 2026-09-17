package com.dulpick.app.data.profile.remote.dto

import kotlinx.serialization.Serializable

// MARK: - 요청

// 온보딩 최초 프로필 초기화(POST /members/me/profile)
@Serializable
data class InitializeMemberProfileRequestDto(
    val nickname: String,
    val profileIcon: Int,
    // 닉네임 단계에서는 성향을 아직 묻지 않는다. null 이면 explicitNulls=false 로 키가 빠진다
    val datePreferences: DatePreferencesRequestDto? = null,
)

// 이미 온보딩된 회원의 프로필 수정(PATCH /members/me/profile)
@Serializable
data class UpdateMemberProfileRequestDto(
    val nickname: String,
    val profileIcon: Int,
)

@Serializable
data class DatePreferencesRequestDto(
    val indoorOutdoor: String,
    val activityLevel: String,
    val dateTime: String,
    val dateFocus: String,
)

@Serializable
data class NotificationSettingsRequestDto(
    val contentSavedEnabled: Boolean,
    val dateScheduleEnabled: Boolean,
    val marketingEnabled: Boolean,
    // 마케팅을 켤 때 동의한 약관 버전. null 이면 키가 빠진다
    val marketingConsentVersion: String? = null,
)

// MARK: - 응답

@Serializable
data class InitializedMemberProfileResponseDto(
    val nickname: String,
    val profileIcon: Int,
    val datePreferences: MemberDatePreferencesResponseDto? = null,
    val connectionCode: String? = null,
    val shareUrl: String? = null,
)

@Serializable
data class UpdatedMemberProfileResponseDto(
    val nickname: String,
    val profileIcon: Int,
)

@Serializable
data class NotificationSettingsResponseDto(
    val contentSavedEnabled: Boolean,
    val dateScheduleEnabled: Boolean,
    val marketingEnabled: Boolean,
    val marketingConsentVersion: String? = null,
    val availableMarketingConsentVersion: String? = null,
)
