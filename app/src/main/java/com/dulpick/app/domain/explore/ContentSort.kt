package com.dulpick.app.domain.explore

// 게시물 정렬 기준. 미등록 사용자는 인기, datePreference 를 등록한 사용자는 성향 (iOS ContentSort 대응)
enum class ContentSort(val serverValue: String) {
    POPULAR("POPULAR"),
    PREFERENCE("PREFERENCE"),
}
