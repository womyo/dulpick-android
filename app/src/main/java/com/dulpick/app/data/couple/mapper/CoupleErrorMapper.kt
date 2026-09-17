package com.dulpick.app.data.couple.mapper

import com.dulpick.app.core.network.NetworkError
import com.dulpick.app.domain.couple.CoupleError

object CoupleErrorMapper {
    private const val HTTP_BAD_REQUEST = 400
    private const val HTTP_NOT_FOUND = 404
    private const val HTTP_CONFLICT = 409
    private const val HTTP_UNPROCESSABLE = 422
    private const val HTTP_TOO_MANY_REQUESTS = 429

    fun map(error: Throwable): CoupleError =
        when (error) {
            is CoupleError -> error
            is NetworkError -> mapNetworkError(error)
            else -> CoupleError.Unknown
        }

    private fun mapNetworkError(error: NetworkError): CoupleError =
        when (error) {
            NetworkError.Network -> CoupleError.Network
            NetworkError.Unauthorized -> CoupleError.Unauthorized
            is NetworkError.Server -> when (error.code) {
                HTTP_BAD_REQUEST, HTTP_NOT_FOUND, HTTP_UNPROCESSABLE -> CoupleError.InvalidInviteCode
                HTTP_CONFLICT -> CoupleError.AlreadyConnected
                HTTP_TOO_MANY_REQUESTS -> CoupleError.RateLimited
                else -> CoupleError.Unknown
            }
            NetworkError.Serialization, NetworkError.Unknown -> CoupleError.Unknown
        }
}
