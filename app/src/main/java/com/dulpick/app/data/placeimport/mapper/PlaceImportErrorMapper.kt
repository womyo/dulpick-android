package com.dulpick.app.data.placeimport.mapper

import com.dulpick.app.core.network.NetworkError
import com.dulpick.app.domain.placeimport.PlaceImportError
import kotlinx.coroutines.CancellationException

object PlaceImportErrorMapper {
    fun map(error: Throwable): PlaceImportError =
        when (error) {
            // 코루틴 취소는 오류가 아니다. Unknown 으로 바꾸면 폴링 취소 처리가 깨진다
            is CancellationException -> throw error
            is PlaceImportError -> error
            is NetworkError -> mapNetworkError(error)
            else -> PlaceImportError.Unknown
        }

    private fun mapNetworkError(error: NetworkError): PlaceImportError =
        when (error) {
            NetworkError.Unauthorized -> PlaceImportError.Unauthorized
            NetworkError.Network -> PlaceImportError.Network
            is NetworkError.Server -> when (error.code) {
                HTTP_FORBIDDEN -> PlaceImportError.Forbidden
                HTTP_NOT_FOUND -> PlaceImportError.NotFound
                HTTP_TOO_MANY_REQUESTS -> PlaceImportError.RateLimited
                else -> PlaceImportError.Unknown
            }
            else -> PlaceImportError.Unknown
        }

    private const val HTTP_FORBIDDEN = 403
    private const val HTTP_NOT_FOUND = 404
    private const val HTTP_TOO_MANY_REQUESTS = 429
}
