package com.dulpick.app.data.place.mapper

import com.dulpick.app.core.network.NetworkError
import com.dulpick.app.domain.place.PlaceError
import kotlinx.coroutines.CancellationException

object PlaceErrorMapper {
    fun map(error: Throwable): PlaceError =
        when (error) {
            // 코루틴 취소는 오류가 아니다. 그대로 전파한다
            is CancellationException -> throw error
            is PlaceError -> error
            is NetworkError -> mapNetworkError(error)
            else -> PlaceError.Unknown
        }

    private fun mapNetworkError(error: NetworkError): PlaceError =
        when (error) {
            NetworkError.Unauthorized -> PlaceError.Unauthorized
            NetworkError.Network -> PlaceError.Network
            is NetworkError.Server -> when (error.code) {
                HTTP_NOT_FOUND -> PlaceError.NotFound
                HTTP_CONFLICT -> PlaceError.AlreadySaved
                else -> PlaceError.Unknown
            }
            else -> PlaceError.Unknown
        }

    private const val HTTP_NOT_FOUND = 404
    private const val HTTP_CONFLICT = 409
}
