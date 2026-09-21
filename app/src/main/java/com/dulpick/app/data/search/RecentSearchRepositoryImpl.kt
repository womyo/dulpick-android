package com.dulpick.app.data.search

import com.dulpick.app.data.search.local.RecentSearchLocalDataSource
import com.dulpick.app.domain.search.RecentSearchRepository

// 최근 검색어 정책만 담당한다. 저장/조회 I/O 는 LocalDataSource 에 위임한다.
// 저장소는 DI 에서 용도별(탐색·지도)로 다른 파일을 주입한다
class RecentSearchRepositoryImpl(
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
