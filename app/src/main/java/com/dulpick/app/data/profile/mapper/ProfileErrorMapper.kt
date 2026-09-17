package com.dulpick.app.data.profile.mapper

import com.dulpick.app.core.network.NetworkError
import com.dulpick.app.domain.profile.ProfileError

object ProfileErrorMapper {
    private const val HTTP_BAD_REQUEST = 400
    private const val HTTP_CONFLICT = 409
    private const val HTTP_UNPROCESSABLE = 422

    fun map(error: Throwable): ProfileError =
        when (error) {
            is ProfileError -> error
            is NetworkError -> mapNetworkError(error)
            else -> ProfileError.Unknown
        }

    private fun mapNetworkError(error: NetworkError): ProfileError =
        when (error) {
            NetworkError.Network -> ProfileError.Network
            NetworkError.Unauthorized -> ProfileError.Unauthorized
            is NetworkError.Server -> when (error.code) {
                HTTP_BAD_REQUEST, HTTP_CONFLICT, HTTP_UNPROCESSABLE -> ProfileError.InvalidNickname
                else -> ProfileError.Unknown
            }
            NetworkError.Serialization, NetworkError.Unknown -> ProfileError.Unknown
        }
}
