package com.dulpick.app.data.profile.remote

import com.dulpick.app.data.profile.remote.dto.DatePreferencesRequestDto
import com.dulpick.app.data.profile.remote.dto.InitializeMemberProfileRequestDto
import com.dulpick.app.data.profile.remote.dto.InitializedMemberProfileResponseDto
import com.dulpick.app.data.profile.remote.dto.MemberResponseDto
import com.dulpick.app.data.profile.remote.dto.NotificationSettingsRequestDto
import com.dulpick.app.data.profile.remote.dto.NotificationSettingsResponseDto
import com.dulpick.app.data.profile.remote.dto.UpdateMemberProfileRequestDto
import com.dulpick.app.data.profile.remote.dto.UpdatedMemberProfileResponseDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT

interface ProfileApi {
    @GET("/api/v1/members/me")
    suspend fun member(): MemberResponseDto

    // 회원 탈퇴
    @DELETE("/api/v1/members/me")
    suspend fun withdraw()

    @GET("/api/v1/members/me/notification-settings")
    suspend fun notificationSettings(): NotificationSettingsResponseDto

    @PUT("/api/v1/members/me/notification-settings")
    suspend fun updateNotificationSettings(
        @Body body: NotificationSettingsRequestDto,
    ): NotificationSettingsResponseDto

    @POST("/api/v1/members/me/profile")
    suspend fun initializeProfile(
        @Body body: InitializeMemberProfileRequestDto,
    ): InitializedMemberProfileResponseDto

    @PATCH("/api/v1/members/me/profile")
    suspend fun updateProfile(
        @Body body: UpdateMemberProfileRequestDto,
    ): UpdatedMemberProfileResponseDto

    @PUT("/api/v1/members/me/date-preferences")
    suspend fun updateDatePreferences(@Body body: DatePreferencesRequestDto)
}
