package com.dulpick.app.data.search.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// 최근 검색어의 실제 저장/조회만 담당. 중복 제거·정렬·개수 제한 정책은 Repository 가 갖는다.
// 저장 파일(fileName)만 달리하면 탐색·지도 등 용도별로 따로 보관한다 (iOS 별도 client 대응)
class RecentSearchLocalDataSource(
    context: Context,
    fileName: String,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(fileName, Context.MODE_PRIVATE)

    suspend fun load(): List<String> = withContext(Dispatchers.IO) {
        val raw = prefs.getString(KEY_TERMS, null) ?: return@withContext emptyList()
        runCatching { Json.decodeFromString<List<String>>(raw) }.getOrDefault(emptyList())
    }

    suspend fun save(terms: List<String>): Unit = withContext(Dispatchers.IO) {
        prefs.edit().putString(KEY_TERMS, Json.encodeToString(terms)).apply()
    }

    suspend fun clear(): Unit = withContext(Dispatchers.IO) {
        prefs.edit().remove(KEY_TERMS).apply()
    }

    private companion object {
        const val KEY_TERMS = "recent_terms"
    }
}
