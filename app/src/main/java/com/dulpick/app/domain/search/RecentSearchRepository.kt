package com.dulpick.app.domain.search

// 최근 검색어 로컬 저장. 각 메서드는 갱신된 목록(최신순)을 돌려준다 (iOS recentSearchClient 대응)
interface RecentSearchRepository {
    suspend fun recent(): List<String>
    suspend fun add(query: String): List<String>
    suspend fun remove(term: String): List<String>
    suspend fun clear()
}
