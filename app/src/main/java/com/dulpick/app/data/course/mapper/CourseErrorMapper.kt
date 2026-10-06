package com.dulpick.app.data.course.mapper

import com.dulpick.app.core.network.NetworkError
import com.dulpick.app.domain.course.CourseError
import kotlinx.coroutines.CancellationException

// 명세에 코스 에러 응답이 없다. 서버 code 문자열을 안 읽고 HTTP 상태만 본다 (iOS CourseErrorMapper 대응)
object CourseErrorMapper {
    fun map(error: Throwable): CourseError =
        when (error) {
            // 코루틴 취소는 오류가 아니다. 그대로 전파한다
            is CancellationException -> throw error
            is CourseError -> error
            is NetworkError -> mapNetworkError(error)
            else -> CourseError.Unknown
        }

    private fun mapNetworkError(error: NetworkError): CourseError =
        when (error) {
            NetworkError.Unauthorized -> CourseError.Unauthorized
            NetworkError.Network -> CourseError.Network
            is NetworkError.Server -> when (error.code) {
                HTTP_NOT_FOUND -> CourseError.NotFound
                HTTP_CONFLICT -> CourseError.Conflict
                else -> CourseError.Unknown
            }
            else -> CourseError.Unknown
        }

    private const val HTTP_NOT_FOUND = 404
    private const val HTTP_CONFLICT = 409
}
