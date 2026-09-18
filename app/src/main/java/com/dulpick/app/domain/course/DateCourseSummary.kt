package com.dulpick.app.domain.course

// 예정 데이트 요약. 홈 배너가 제목과 장소 수를 읽는다 (iOS DateCourseSummary 대응, 화면에 필요한 필드만)
data class DateCourseSummary(
    val id: String,
    val title: String,
    val totalPlaceCount: Int,
)
