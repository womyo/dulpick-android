package com.dulpick.app.data.auth.mapper

import com.dulpick.app.core.network.NetworkError
import com.dulpick.app.core.social.SocialAuthException
import com.dulpick.app.core.storage.StorageException
import com.dulpick.app.domain.auth.AuthError
import kotlinx.serialization.SerializationException

// 인프라 에러를 도메인 AuthError 로 좁힌다
object AuthErrorMapper {

    fun map(error: Throwable, isLoginPath: Boolean = false): AuthError = when (error) {
        is AuthError -> error
        is SocialAuthException -> mapSocial(error)
        is NetworkError -> mapNetwork(error, isLoginPath)
        is StorageException -> AuthError.Storage
        is SerializationException -> AuthError.Storage
        else -> AuthError.Unknown
    }

    private fun mapSocial(error: SocialAuthException): AuthError = when (error) {
        SocialAuthException.Cancelled -> AuthError.Cancelled
        SocialAuthException.Failed -> AuthError.LoginFailed
    }

    private fun mapNetwork(error: NetworkError, isLoginPath: Boolean): AuthError = when (error) {
        NetworkError.Network -> AuthError.Network
        NetworkError.Unauthorized -> AuthError.Unauthorized
        is NetworkError.Server -> if (isLoginPath) AuthError.LoginFailed else AuthError.Unknown
        NetworkError.Serialization -> AuthError.Unknown
        NetworkError.Unknown -> AuthError.Unknown
    }
}
