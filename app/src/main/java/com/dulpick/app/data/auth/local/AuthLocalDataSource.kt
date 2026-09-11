package com.dulpick.app.data.auth.local

import com.dulpick.app.core.storage.SecureStorage
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

class AuthLocalDataSource @Inject constructor(
    private val storage: SecureStorage,
    private val json: Json,
) {
    // 세션 읽기-수정-쓰기를 직렬화한다. 토큰 회전·온보딩 갱신·로그아웃이 서로 덮어쓰거나 삭제본을 되살리지 않게 한다
    private val mutex = Mutex()

    suspend fun loadSession(): AuthSessionEntity? = mutex.withLock { readUnlocked() }

    suspend fun saveSession(session: AuthSessionEntity): Unit = mutex.withLock { writeUnlocked(session) }

    // 온보딩 플래그만 갱신. 토큰은 저장된 최신 값을 그대로 둔다
    suspend fun updateOnboardingCompleted(value: Boolean): Unit = mutex.withLock {
        val current = readUnlocked() ?: return@withLock
        writeUnlocked(current.copy(isOnboardingCompleted = value))
    }

    suspend fun deleteSession(): Unit = mutex.withLock { storage.remove(KEY) }

    // 토큰 회전을 원자적으로 반영한다.
    // 저장된 세션이 사라졌거나(로그아웃) refreshToken 이 이미 바뀌었으면(다른 회전) 되살리지 않고 null 을 낸다.
    // 성공 시 최신 저장본의 온보딩 플래그를 보존한 채 토큰만 교체한다
    suspend fun rotateTokens(
        expectedRefreshToken: String,
        newAccessToken: String,
        newRefreshToken: String,
    ): AuthSessionEntity? = mutex.withLock {
        val current = readUnlocked() ?: return@withLock null
        if (current.refreshToken != expectedRefreshToken) return@withLock null
        val rotated = current.copy(accessToken = newAccessToken, refreshToken = newRefreshToken)
        writeUnlocked(rotated)
        rotated
    }

    private suspend fun readUnlocked(): AuthSessionEntity? =
        storage.getString(KEY)?.let { json.decodeFromString<AuthSessionEntity>(it) }

    private suspend fun writeUnlocked(session: AuthSessionEntity) {
        storage.putString(KEY, json.encodeToString(session))
    }

    private companion object {
        const val KEY = "auth-session"
    }
}
