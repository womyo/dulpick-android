package com.dulpick.app.data.home.mapper

import com.dulpick.app.core.network.NetworkError
import com.dulpick.app.domain.home.HomeError

object HomeErrorMapper {
    fun map(error: Throwable): HomeError =
        when (error) {
            is HomeError -> error
            is NetworkError -> mapNetworkError(error)
            else -> HomeError.Unknown
        }

    private fun mapNetworkError(error: NetworkError): HomeError =
        when (error) {
            NetworkError.Unauthorized -> HomeError.Unauthorized
            NetworkError.Network -> HomeError.Network
            else -> HomeError.Unknown
        }
}
