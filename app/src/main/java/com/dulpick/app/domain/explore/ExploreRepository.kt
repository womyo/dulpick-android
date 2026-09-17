package com.dulpick.app.domain.explore

// Feature 는 이 인터페이스로만 탐색 데이터에 접근한다
interface ExploreRepository {
    // 게시물 목록 (페이지네이션). 기본 인기순, 홈 추천은 성향순도 쓴다
    suspend fun contents(page: Int, size: Int, sort: ContentSort = ContentSort.POPULAR): ContentPage

    // 태그/검색어로 게시물 검색 (페이지네이션)
    suspend fun searchContents(query: String, page: Int, size: Int): ContentPage
}
