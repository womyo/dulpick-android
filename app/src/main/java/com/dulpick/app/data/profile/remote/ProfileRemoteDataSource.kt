package com.dulpick.app.data.profile.remote

import com.dulpick.app.core.network.Authed
import com.dulpick.app.core.network.safeApiCall
import com.dulpick.app.data.profile.remote.dto.MemberResponseDto
import javax.inject.Inject

// 온보딩 판정을 위한 최소 Profile 조회. authed 클라이언트 사용
class ProfileRemoteDataSource @Inject constructor(
    @Authed private val profileApi: ProfileApi,
) {
    suspend fun member(): MemberResponseDto = safeApiCall { profileApi.member() }
}
