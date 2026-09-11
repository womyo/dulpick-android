package com.dulpick.app.core.storage

// 값은 문자열로 저장하고 상위에서 JSON 직렬화한다
interface SecureStorage {
    suspend fun getString(key: String): String?
    suspend fun putString(key: String, value: String)
    suspend fun remove(key: String)
}
