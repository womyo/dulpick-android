package com.dulpick.app.data.profile

import com.dulpick.app.data.profile.mapper.ProfileDtoMapper
import com.dulpick.app.data.profile.mapper.ProfileErrorMapper
import com.dulpick.app.data.profile.remote.ProfileRemoteDataSource
import com.dulpick.app.data.profile.remote.dto.InitializeMemberProfileRequestDto
import com.dulpick.app.data.profile.remote.dto.UpdateMemberProfileRequestDto
import com.dulpick.app.domain.profile.DatePreference
import com.dulpick.app.domain.profile.NotificationSettings
import com.dulpick.app.domain.profile.ProfileError
import com.dulpick.app.domain.profile.ProfileRepository
import com.dulpick.app.domain.profile.UserProfile
import javax.inject.Inject

@Suppress("TooGenericExceptionCaught")
class ProfileRepositoryImpl @Inject constructor(
    private val profileRemote: ProfileRemoteDataSource,
) : ProfileRepository {

    // 온보딩 전이면 초기화(POST), 이미 온보딩됐으면 수정(PATCH).
    // PATCH 응답엔 성향이 없어 방금 읽은 회원 정보의 성향을 재사용한다
    override suspend fun updateNickname(nickname: String, iconId: Int): UserProfile {
        try {
            val member = profileRemote.member()
            if (!member.onboardingCompleted) {
                val initialized = profileRemote.initializeProfile(
                    InitializeMemberProfileRequestDto(nickname = nickname, profileIcon = iconId),
                )
                return ProfileDtoMapper.toDomain(initialized)
            }
            val updated = profileRemote.updateProfile(
                UpdateMemberProfileRequestDto(nickname = nickname, profileIcon = iconId),
            )
            return ProfileDtoMapper.toDomain(
                updated,
                datePreference = ProfileDtoMapper.toDatePreference(member.datePreferences),
            )
        } catch (error: Throwable) {
            throw ProfileErrorMapper.map(error)
        }
    }

    override suspend fun updateDatePreference(preference: DatePreference): UserProfile {
        try {
            profileRemote.updateDatePreferences(ProfileDtoMapper.toRequest(preference))
            val member = profileRemote.member()
            return ProfileDtoMapper.toDomain(member) ?: throw ProfileError.Unknown
        } catch (error: Throwable) {
            throw ProfileErrorMapper.map(error)
        }
    }

    override suspend fun notificationSettings(): NotificationSettings {
        try {
            return ProfileDtoMapper.toDomain(profileRemote.notificationSettings())
        } catch (error: Throwable) {
            throw ProfileErrorMapper.map(error)
        }
    }

    override suspend fun updateNotificationSettings(
        settings: NotificationSettings,
    ): NotificationSettings {
        try {
            val dto = profileRemote.updateNotificationSettings(ProfileDtoMapper.toRequest(settings))
            return ProfileDtoMapper.toDomain(dto)
        } catch (error: Throwable) {
            throw ProfileErrorMapper.map(error)
        }
    }
}
