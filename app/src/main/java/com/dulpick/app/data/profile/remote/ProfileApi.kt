package com.dulpick.app.data.profile.remote

import com.dulpick.app.data.profile.remote.dto.MemberResponseDto
import retrofit2.http.GET

interface ProfileApi {
    @GET("/api/v1/members/me")
    suspend fun member(): MemberResponseDto
}
