package com.dulpick.app.data.home.mapper

import com.dulpick.app.core.network.NetworkError
import com.dulpick.app.domain.home.HomeError
import kotlinx.coroutines.CancellationException

object HomeErrorMapper {
    fun map(error: Throwable): HomeError =
        when (error) {
            // 코루틴 취소는 오류가 아니다. Unknown 으로 바꾸면 ViewModel 의 취소 재전파가 무력화된다
            is CancellationException -> throw error
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
