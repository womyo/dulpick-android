package com.dulpick.app.data.search

import com.dulpick.app.data.search.local.RecentSearchLocalDataSource
import com.dulpick.app.domain.search.RecentSearchRepository
import javax.inject.Inject
import javax.inject.Singleton

// 최근 검색어 정책만 담당한다. 저장/조회 I/O 는 LocalDataSource 에 위임한다
@Singleton
class RecentSearchRepositoryImpl @Inject constructor(
    private val local: RecentSearchLocalDataSource,
) : RecentSearchRepository {

    override suspend fun recent(): List<String> = local.load()

    // 중복은 앞으로 끌어올리고 최대 개수로 자른다
    override suspend fun add(query: String): List<String> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return local.load()
        val next = (listOf(trimmed) + local.load().filterNot { it == trimmed }).take(MAX_COUNT)
        local.save(next)
        return next
    }

    override suspend fun remove(term: String): List<String> {
        val next = local.load().filterNot { it == term }
        local.save(next)
        return next
    }

    override suspend fun clear() = local.clear()

    private companion object {
        const val MAX_COUNT = 10
    }
}
