package com.dulpick.app.data.notification

import com.dulpick.app.core.network.NetworkError
import com.dulpick.app.data.notification.local.PushLocalDataSource
import com.dulpick.app.data.notification.mapper.PushDtoMapper
import com.dulpick.app.data.notification.mapper.PushErrorMapper
import com.dulpick.app.data.notification.remote.PushRemoteDataSource
import com.dulpick.app.domain.notification.PushDeviceRepository
import javax.inject.Inject

@Suppress("TooGenericExceptionCaught")
class PushDeviceRepositoryImpl @Inject constructor(
    private val pushRemote: PushRemoteDataSource,
    private val pushLocal: PushLocalDataSource,
) : PushDeviceRepository {

    override suspend fun registerDevice(token: String) {
        try {
            pushRemote.register(pushLocal.deviceId(), PushDtoMapper.toRequest(token))
        } catch (error: Throwable) {
            throw PushErrorMapper.map(error)
        }
    }

    override suspend fun unregisterDevice() {
        try {
            pushRemote.unregister(pushLocal.deviceId())
        } catch (error: NetworkError.Server) {
            // 등록된 기기가 없으면 404 가 온다. 해제 목적은 이미 이뤄진 상태다
            if (error.code != HTTP_NOT_FOUND) throw PushErrorMapper.map(error)
        } catch (error: Throwable) {
            throw PushErrorMapper.map(error)
        }
    }

    private companion object {
        const val HTTP_NOT_FOUND = 404
    }
}
