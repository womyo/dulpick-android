package com.dulpick.app.domain.home

// 지난 데이트 코스 한 페이지. totalCount 는 전체 데이트 횟수 (iOS PastDateCoursePage 대응)
data class PastDateCoursePage(
    val courses: List<DateSchedule>,
    val totalCount: Int,
    val hasNext: Boolean,
)
