package com.dulpick.app.data.auth.mapper

import com.dulpick.app.data.auth.local.AuthSessionEntity
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

    fun toDomain(entity: AuthSessionEntity): AuthSession =
        AuthSession(
            accessToken = entity.accessToken,
            refreshToken = entity.refreshToken,
            userId = entity.userId,
        )
}
