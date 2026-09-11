package com.dulpick.app.data.auth.mapper

import com.dulpick.app.data.auth.local.AuthSessionEntity
import com.dulpick.app.data.auth.remote.dto.AuthTokenDto
import com.dulpick.app.data.auth.remote.dto.SocialLoginResponseDto
import com.dulpick.app.domain.auth.AuthSession

object AuthMapper {

    fun toEntity(response: SocialLoginResponseDto): AuthSessionEntity =
        AuthSessionEntity(
            accessToken = response.token.accessToken,
            refreshToken = response.token.refreshToken,
            userId = response.memberId.toString(),
            isOnboardingCompleted = response.onboardingCompleted,
        )

    // 토큰 회전. 기존 온보딩 플래그를 그대로 실어야 다음 복원의 백업 값이 남는다
    fun toEntity(token: AuthTokenDto, current: AuthSessionEntity): AuthSessionEntity =
        current.copy(
            accessToken = token.accessToken,
            refreshToken = token.refreshToken,
        )

    fun toDomain(entity: AuthSessionEntity): AuthSession =
        AuthSession(
            accessToken = entity.accessToken,
            refreshToken = entity.refreshToken,
            userId = entity.userId,
        )
}
