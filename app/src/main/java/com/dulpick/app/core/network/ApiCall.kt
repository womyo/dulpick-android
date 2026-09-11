package com.dulpick.app.core.network

import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException

// Retrofit/OkHttp 예외를 NetworkError 로 좁힌다. 데이터소스 호출을 이걸로 감싼다
@Suppress("TooGenericExceptionCaught")
suspend fun <T> safeApiCall(block: suspend () -> T): T =
    try {
        block()
    } catch (error: NetworkError) {
        throw error
    } catch (error: HttpException) {
        throw if (error.code() == HTTP_UNAUTHORIZED) {
            NetworkError.Unauthorized
        } else {
            NetworkError.Server(error.code())
        }
    } catch (error: IOException) {
        throw NetworkError.Network
    } catch (error: SerializationException) {
        throw NetworkError.Serialization
    } catch (error: Exception) {
        throw NetworkError.Unknown
    }

private const val HTTP_UNAUTHORIZED = 401
