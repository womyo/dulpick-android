package com.dulpick.app.domain.home

import com.dulpick.app.domain.course.DateCourseSummary

// 홈 상단 요약(GET /api/v1/home). 연결 여부·닉네임·현재 데이트 코스(배너용) (iOS HomeSummary 대응)
data class HomeSummary(
    val connected: Boolean,
    val myNickname: String,
    val partnerNickname: String?,
    val currentDateCourse: DateCourseSummary?,
)
