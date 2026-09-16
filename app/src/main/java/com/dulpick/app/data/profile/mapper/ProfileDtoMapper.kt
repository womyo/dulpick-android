package com.dulpick.app.data.profile.mapper

import com.dulpick.app.data.profile.remote.dto.DatePreferencesRequestDto
import com.dulpick.app.data.profile.remote.dto.InitializedMemberProfileResponseDto
import com.dulpick.app.data.profile.remote.dto.MemberDatePreferencesResponseDto
import com.dulpick.app.data.profile.remote.dto.MemberResponseDto
import com.dulpick.app.data.profile.remote.dto.NotificationSettingsRequestDto
import com.dulpick.app.data.profile.remote.dto.NotificationSettingsResponseDto
import com.dulpick.app.data.profile.remote.dto.UpdatedMemberProfileResponseDto
import com.dulpick.app.domain.profile.ActivityLevel
import com.dulpick.app.domain.profile.DateFocus
import com.dulpick.app.domain.profile.DatePreference
import com.dulpick.app.domain.profile.DateTime
import com.dulpick.app.domain.profile.IndoorOutdoor
import com.dulpick.app.domain.profile.NotificationSettings
import com.dulpick.app.domain.profile.UserProfile

object ProfileDtoMapper {
    private const val DEFAULT_ICON_ID = 1

    fun toDomain(dto: InitializedMemberProfileResponseDto): UserProfile =
        UserProfile(
            nickname = dto.nickname,
            iconId = dto.profileIcon,
            datePreference = toDatePreference(dto.datePreferences),
        )

    fun toDomain(
        dto: UpdatedMemberProfileResponseDto,
        datePreference: DatePreference?,
    ): UserProfile =
        UserProfile(
            nickname = dto.nickname,
            iconId = dto.profileIcon,
            datePreference = datePreference,
        )

    // 닉네임이 없으면 온보딩 전이라 프로필로 볼 수 없다
    fun toDomain(dto: MemberResponseDto): UserProfile? {
        val nickname = dto.nickname ?: return null
        return UserProfile(
            nickname = nickname,
            iconId = dto.profileIcon ?: DEFAULT_ICON_ID,
            datePreference = toDatePreference(dto.datePreferences),
        )
    }

    // 4축이 모두 파싱될 때만 성향을 만든다. 부분 성향은 Domain 에 없는 상태다
    fun toDatePreference(dto: MemberDatePreferencesResponseDto?): DatePreference? {
        dto ?: return null
        return buildPreference(
            IndoorOutdoor.from(dto.indoorOutdoor),
            ActivityLevel.from(dto.activityLevel),
            DateTime.from(dto.dateTime),
            DateFocus.from(dto.dateFocus),
        )
    }

    private fun buildPreference(
        indoorOutdoor: IndoorOutdoor?,
        activityLevel: ActivityLevel?,
        dateTime: DateTime?,
        dateFocus: DateFocus?,
    ): DatePreference? {
        if (indoorOutdoor == null || activityLevel == null) return null
        if (dateTime == null || dateFocus == null) return null
        return DatePreference(indoorOutdoor, activityLevel, dateTime, dateFocus)
    }

    fun toDomain(dto: NotificationSettingsResponseDto): NotificationSettings =
        NotificationSettings(
            contentSavedEnabled = dto.contentSavedEnabled,
            dateScheduleEnabled = dto.dateScheduleEnabled,
            marketingEnabled = dto.marketingEnabled,
            marketingConsentVersion = dto.marketingConsentVersion,
            availableMarketingConsentVersion = dto.availableMarketingConsentVersion,
        )

    fun toRequest(settings: NotificationSettings): NotificationSettingsRequestDto =
        NotificationSettingsRequestDto(
            contentSavedEnabled = settings.contentSavedEnabled,
            dateScheduleEnabled = settings.dateScheduleEnabled,
            marketingEnabled = settings.marketingEnabled,
            marketingConsentVersion = settings.marketingConsentVersion,
        )

    fun toRequest(preference: DatePreference): DatePreferencesRequestDto =
        DatePreferencesRequestDto(
            indoorOutdoor = preference.indoorOutdoor.rawValue,
            activityLevel = preference.activityLevel.rawValue,
            dateTime = preference.dateTime.rawValue,
            dateFocus = preference.dateFocus.rawValue,
        )
}
