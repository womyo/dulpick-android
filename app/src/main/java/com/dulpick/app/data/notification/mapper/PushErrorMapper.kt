package com.dulpick.app.data.notification.mapper

import com.dulpick.app.core.network.NetworkError
import com.dulpick.app.domain.notification.PushError
import kotlinx.coroutines.CancellationException

object PushErrorMapper {
    private const val BAD_REQUEST = 400
    private const val CONFLICT = 409
    private const val SERVICE_UNAVAILABLE = 503

    fun map(error: Throwable): Throwable =
        when (error) {
            // 코루틴 취소는 오류가 아니다. 바꾸면 등록 구독의 취소가 무력화된다
            is CancellationException -> throw error
            is PushError -> error
            NetworkError.Network -> PushError.Network
            NetworkError.Unauthorized -> PushError.Unauthorized
            is NetworkError.Server -> mapServer(error.code)
            else -> PushError.Unknown
        }

    private fun mapServer(code: Int): PushError =
        when (code) {
            BAD_REQUEST -> PushError.InvalidRequest
            CONFLICT -> PushError.RegistrationConflict
            SERVICE_UNAVAILABLE -> PushError.ProviderUnavailable
            else -> PushError.Unknown
        }
}
