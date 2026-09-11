package com.dulpick.app.domain.auth

// 구현은 data/AuthRepositoryImpl, 바인딩은 di/
interface AuthRepository {
    suspend fun restoreSession(): AuthBootstrap?
    suspend fun login(provider: AuthProvider): AuthBootstrap
    suspend fun logout()
    suspend fun currentSession(): AuthSession?
}
