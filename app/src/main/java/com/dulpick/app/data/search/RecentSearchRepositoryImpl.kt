package com.dulpick.app.data.search

import android.content.Context
import android.content.SharedPreferences
import com.dulpick.app.domain.search.RecentSearchRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

// 최근 검색어를 SharedPreferences 에 JSON 배열로 보관한다. 민감 정보가 아니라 일반 prefs 사용
@Singleton
class RecentSearchRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
) : RecentSearchRepository {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    override suspend fun recent(): List<String> = withContext(Dispatchers.IO) { load() }

    // 중복은 앞으로 끌어올리고 최대 개수로 자른다
    override suspend fun add(query: String): List<String> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@withContext load()
        val next = (listOf(trimmed) + load().filterNot { it == trimmed }).take(MAX_COUNT)
        save(next)
        next
    }

    override suspend fun remove(term: String): List<String> = withContext(Dispatchers.IO) {
        val next = load().filterNot { it == term }
        save(next)
        next
    }

    override suspend fun clear(): Unit = withContext(Dispatchers.IO) {
        prefs.edit().remove(KEY_TERMS).apply()
    }

    private fun load(): List<String> {
        val raw = prefs.getString(KEY_TERMS, null) ?: return emptyList()
        return runCatching { Json.decodeFromString<List<String>>(raw) }.getOrDefault(emptyList())
    }

    private fun save(terms: List<String>) {
        prefs.edit().putString(KEY_TERMS, Json.encodeToString(terms)).apply()
    }

    private companion object {
        const val FILE_NAME = "dulpick_recent_search"
        const val KEY_TERMS = "recent_terms"
        const val MAX_COUNT = 10
    }
}
