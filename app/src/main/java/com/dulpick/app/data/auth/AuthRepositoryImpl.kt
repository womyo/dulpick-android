package com.dulpick.app.data.auth

import com.dulpick.app.core.network.NetworkError
import com.dulpick.app.data.auth.local.AuthLocalDataSource
import com.dulpick.app.data.auth.local.AuthSessionEntity
import com.dulpick.app.data.auth.mapper.AuthErrorMapper
import com.dulpick.app.data.auth.mapper.AuthMapper
import com.dulpick.app.data.auth.remote.AuthRemoteDataSource
import com.dulpick.app.data.auth.social.SocialAuthProvider
import com.dulpick.app.data.profile.remote.ProfileRemoteDataSource
import com.dulpick.app.domain.auth.AuthBootstrap
import com.dulpick.app.domain.auth.AuthError
import com.dulpick.app.domain.auth.AuthProvider
import com.dulpick.app.domain.auth.AuthRepository
import com.dulpick.app.domain.auth.AuthSession
import javax.inject.Inject

@Suppress("TooGenericExceptionCaught", "SwallowedException")
class AuthRepositoryImpl @Inject constructor(
    private val authRemote: AuthRemoteDataSource,
    private val authLocal: AuthLocalDataSource,
    private val socialAuth: SocialAuthProvider,
    private val profileRemote: ProfileRemoteDataSource,
) : AuthRepository {

    override suspend fun restoreSession(): AuthBootstrap? {
        try {
            val stored = authLocal.loadSession() ?: return null
            return AuthBootstrap(
                session = AuthMapper.toDomain(stored),
                isOnboardingCompleted = resolveOnboardingCompleted(stored),
                isNewMember = false,
            )
        } catch (error: Throwable) {
            throw AuthErrorMapper.map(error)
        }
    }

    // 서버 값이 우선. 실패하면 저장된 플래그로 버티고, 성공하면 그 값을 다시 저장한다
    private suspend fun resolveOnboardingCompleted(stored: AuthSessionEntity): Boolean =
        try {
            val completed = profileRemote.member().onboardingCompleted
            authLocal.updateOnboardingCompleted(completed)
            completed
        } catch (error: NetworkError.Unauthorized) {
            throw AuthError.Unauthorized
        } catch (error: Throwable) {
            stored.isOnboardingCompleted ?: false
        }

    override suspend fun login(provider: AuthProvider): AuthBootstrap {
        try {
            val nonce = authRemote.issueNonce(provider).nonce
            val credential = socialAuth.login(provider, nonce)
            val response = authRemote.socialLogin(
                provider = provider,
                idToken = credential.idToken,
                authorizationCode = credential.authorizationCode,
                nonce = nonce,
            )
            val entity = AuthMapper.toEntity(response)
            authLocal.saveSession(entity)
            return AuthBootstrap(
                session = AuthMapper.toDomain(entity),
                isOnboardingCompleted = entity.isOnboardingCompleted ?: false,
                isNewMember = response.newMember,
            )
        } catch (error: Throwable) {
            throw AuthErrorMapper.map(error, isLoginPath = true)
        }
    }

    override suspend fun logout() {
        try {
            val session = authLocal.loadSession()
            if (session != null) {
                // 서버 로그아웃 실패는 삼킨다. 로컬 삭제는 반드시 한다
                runCatching { authRemote.logout(session.refreshToken) }
            }
            authLocal.deleteSession()
        } catch (error: Throwable) {
            throw AuthErrorMapper.map(error)
        }
    }

    override suspend fun currentSession(): AuthSession? {
        try {
            return authLocal.loadSession()?.let { AuthMapper.toDomain(it) }
        } catch (error: Throwable) {
            throw AuthErrorMapper.map(error)
        }
    }
}
