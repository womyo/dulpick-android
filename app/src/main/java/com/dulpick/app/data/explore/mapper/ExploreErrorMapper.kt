package com.dulpick.app.data.explore.mapper

import com.dulpick.app.core.network.NetworkError
import com.dulpick.app.domain.explore.ExploreError

object ExploreErrorMapper {
    fun map(error: Throwable): ExploreError =
        when (error) {
            is ExploreError -> error
            is NetworkError -> mapNetworkError(error)
            else -> ExploreError.Unknown
        }

    private fun mapNetworkError(error: NetworkError): ExploreError =
        when (error) {
            NetworkError.Unauthorized -> ExploreError.Unauthorized
            NetworkError.Network -> ExploreError.Network
            else -> ExploreError.Unknown
        }
}
