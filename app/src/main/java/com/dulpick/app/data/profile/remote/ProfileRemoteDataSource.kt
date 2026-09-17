package com.dulpick.app.data.profile.remote

import com.dulpick.app.core.network.Authed
import com.dulpick.app.core.network.safeApiCall
import com.dulpick.app.data.profile.remote.dto.DatePreferencesRequestDto
import com.dulpick.app.data.profile.remote.dto.InitializeMemberProfileRequestDto
import com.dulpick.app.data.profile.remote.dto.InitializedMemberProfileResponseDto
import com.dulpick.app.data.profile.remote.dto.MemberResponseDto
import com.dulpick.app.data.profile.remote.dto.NotificationSettingsRequestDto
import com.dulpick.app.data.profile.remote.dto.NotificationSettingsResponseDto
import com.dulpick.app.data.profile.remote.dto.UpdateMemberProfileRequestDto
import com.dulpick.app.data.profile.remote.dto.UpdatedMemberProfileResponseDto
import javax.inject.Inject

// authed 클라이언트로 회원/프로필/성향/알림설정을 호출한다
class ProfileRemoteDataSource @Inject constructor(
    @Authed private val profileApi: ProfileApi,
) {
    suspend fun member(): MemberResponseDto = safeApiCall { profileApi.member() }

    suspend fun withdraw() = safeApiCall { profileApi.withdraw() }

    suspend fun notificationSettings(): NotificationSettingsResponseDto =
        safeApiCall { profileApi.notificationSettings() }

    suspend fun updateNotificationSettings(
        body: NotificationSettingsRequestDto,
    ): NotificationSettingsResponseDto = safeApiCall { profileApi.updateNotificationSettings(body) }

    suspend fun initializeProfile(
        body: InitializeMemberProfileRequestDto,
    ): InitializedMemberProfileResponseDto = safeApiCall { profileApi.initializeProfile(body) }

    suspend fun updateProfile(
        body: UpdateMemberProfileRequestDto,
    ): UpdatedMemberProfileResponseDto = safeApiCall { profileApi.updateProfile(body) }

    suspend fun updateDatePreferences(body: DatePreferencesRequestDto) =
        safeApiCall { profileApi.updateDatePreferences(body) }
}
