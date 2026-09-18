package com.dulpick.app.data.search.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

// 최근 검색어의 실제 저장/조회만 담당. 중복 제거·정렬·개수 제한 정책은 Repository 가 갖는다.
// 민감 정보가 아니라 일반 SharedPreferences 에 JSON 배열로 보관한다
class RecentSearchLocalDataSource @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

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
        const val FILE_NAME = "dulpick_recent_search"
        const val KEY_TERMS = "recent_terms"
    }
}
