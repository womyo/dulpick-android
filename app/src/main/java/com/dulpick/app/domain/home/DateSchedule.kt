package com.dulpick.app.domain.home

// 지난 데이트 일정. 코스짜기 연동 전까지 목 고정 (iOS DateSchedule 대응)
data class DateSchedule(
    val id: String,
    val title: String,
    val placeCount: Int,
    val date: String,
)
