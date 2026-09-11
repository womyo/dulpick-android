package com.dulpick.app.data.auth.local

import com.dulpick.app.core.storage.SecureStorage
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

class AuthLocalDataSource @Inject constructor(
    private val storage: SecureStorage,
    private val json: Json,
) {
    suspend fun loadSession(): AuthSessionEntity? =
        storage.getString(KEY)?.let { json.decodeFromString<AuthSessionEntity>(it) }

    suspend fun saveSession(session: AuthSessionEntity) {
        storage.putString(KEY, json.encodeToString(session))
    }

    // 온보딩 플래그만 갱신. 토큰은 저장된 최신 값을 그대로 둔다
    suspend fun updateOnboardingCompleted(value: Boolean) {
        val current = loadSession() ?: return
        saveSession(current.copy(isOnboardingCompleted = value))
    }

    suspend fun deleteSession() {
        storage.remove(KEY)
    }

    private companion object {
        const val KEY = "auth-session"
    }
}
