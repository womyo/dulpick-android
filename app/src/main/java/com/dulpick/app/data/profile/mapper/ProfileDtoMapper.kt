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
            parseIndoorOutdoor(dto.indoorOutdoor),
            parseActivityLevel(dto.activityLevel),
            parseDateTime(dto.dateTime),
            parseDateFocus(dto.dateFocus),
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
            indoorOutdoor = preference.indoorOutdoor.raw(),
            activityLevel = preference.activityLevel.raw(),
            dateTime = preference.dateTime.raw(),
            dateFocus = preference.dateFocus.raw(),
        )

    // 서버 문자열 계약 ↔ 도메인 enum. 이 매핑을 data 계층에서 소유한다
    private fun parseIndoorOutdoor(raw: String?): IndoorOutdoor? = when (raw) {
        "INDOOR" -> IndoorOutdoor.INDOOR
        "OUTDOOR" -> IndoorOutdoor.OUTDOOR
        else -> null
    }

    private fun parseActivityLevel(raw: String?): ActivityLevel? = when (raw) {
        "ACTIVE" -> ActivityLevel.ACTIVE
        "STATIC" -> ActivityLevel.STATIC
        else -> null
    }

    private fun parseDateTime(raw: String?): DateTime? = when (raw) {
        "DAY" -> DateTime.DAY
        "NIGHT" -> DateTime.NIGHT
        else -> null
    }

    private fun parseDateFocus(raw: String?): DateFocus? = when (raw) {
        "FOOD" -> DateFocus.FOOD
        "SIGHTSEEING" -> DateFocus.SIGHTSEEING
        else -> null
    }

    private fun IndoorOutdoor.raw(): String = when (this) {
        IndoorOutdoor.INDOOR -> "INDOOR"
        IndoorOutdoor.OUTDOOR -> "OUTDOOR"
    }

    private fun ActivityLevel.raw(): String = when (this) {
        ActivityLevel.ACTIVE -> "ACTIVE"
        ActivityLevel.STATIC -> "STATIC"
    }

    private fun DateTime.raw(): String = when (this) {
        DateTime.DAY -> "DAY"
        DateTime.NIGHT -> "NIGHT"
    }

    private fun DateFocus.raw(): String = when (this) {
        DateFocus.FOOD -> "FOOD"
        DateFocus.SIGHTSEEING -> "SIGHTSEEING"
    }
}
