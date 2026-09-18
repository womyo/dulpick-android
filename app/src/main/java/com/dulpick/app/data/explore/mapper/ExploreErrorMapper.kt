package com.dulpick.app.data.explore.mapper

import com.dulpick.app.core.network.NetworkError
import com.dulpick.app.domain.explore.ExploreError
import kotlinx.coroutines.CancellationException

object ExploreErrorMapper {
    fun map(error: Throwable): ExploreError =
        when (error) {
            // 코루틴 취소는 오류가 아니다. Unknown 으로 바꾸면 상위의 취소 재던지기 처리가 깨진다
            is CancellationException -> throw error
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
