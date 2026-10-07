package com.dulpick.app.data.notice.mapper

import com.dulpick.app.core.network.NetworkError
import com.dulpick.app.domain.notice.NoticeError
import kotlinx.coroutines.CancellationException

object NoticeErrorMapper {
    fun map(error: Throwable): Throwable =
        when (error) {
            // 코루틴 취소는 오류가 아니다. 바꾸면 ViewModel 의 취소 재전파가 무력화된다
            is CancellationException -> throw error
            is NoticeError -> error
            NetworkError.Network -> NoticeError.Network
            else -> NoticeError.Unknown
        }
}
