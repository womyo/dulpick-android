package com.dulpick.app.data.couple

import com.dulpick.app.core.network.NetworkError
import com.dulpick.app.data.couple.mapper.CoupleDtoMapper
import com.dulpick.app.data.couple.mapper.CoupleErrorMapper
import com.dulpick.app.data.couple.remote.CoupleRemoteDataSource
import com.dulpick.app.domain.couple.Couple
import com.dulpick.app.domain.couple.CoupleError
import com.dulpick.app.domain.couple.CoupleRepository
import com.dulpick.app.domain.couple.CoupleStatus
import com.dulpick.app.domain.couple.InviteCode
import javax.inject.Inject

@Suppress("TooGenericExceptionCaught")
class CoupleRepositoryImpl @Inject constructor(
    private val coupleRemote: CoupleRemoteDataSource,
) : CoupleRepository {

    private companion object {
        const val HTTP_NOT_FOUND = 404
    }

    override suspend fun inviteCode(): InviteCode {
        try {
            return CoupleDtoMapper.toDomain(coupleRemote.connectionCode())
        } catch (error: Throwable) {
            throw CoupleErrorMapper.map(error)
        }
    }

    override suspend fun connect(inviteCode: String): Couple {
        try {
            val status = coupleRemote.connect(CoupleDtoMapper.toRequest(inviteCode))
            return CoupleDtoMapper.toDomain(status) ?: throw CoupleError.Unknown
        } catch (error: Throwable) {
            throw CoupleErrorMapper.map(error)
        }
    }

    // 명세상 미연결은 200 + connected:false 라 매퍼가 null 로 처리한다.
    // 서버가 커플 없음을 404 로 답하는 경우도 있어 여기서만 null 로 방어한다
    override suspend fun current(): CoupleStatus? {
        try {
            return CoupleDtoMapper.toStatus(coupleRemote.current())
        } catch (error: CoupleError) {
            // 매퍼가 올린 불완전 응답 에러는 그대로 전달
            throw error
        } catch (error: NetworkError.Server) {
            // 서버가 커플 없음을 404 로 답하면 미연결 정상 상태로 본다
            if (error.code == HTTP_NOT_FOUND) return null
            throw CoupleErrorMapper.map(error)
        } catch (error: Throwable) {
            throw CoupleErrorMapper.map(error)
        }
    }

    override suspend fun disconnect() {
        try {
            coupleRemote.disconnect()
        } catch (error: Throwable) {
            throw CoupleErrorMapper.map(error)
        }
    }
}
