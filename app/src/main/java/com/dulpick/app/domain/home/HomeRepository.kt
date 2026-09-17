package com.dulpick.app.domain.home

import com.dulpick.app.domain.place.Place

// Feature 는 이 인터페이스로만 홈 데이터에 접근한다
interface HomeRepository {
    // 홈 상단 요약(연결 여부·닉네임·현재 코스)
    suspend fun home(): HomeSummary

    // 최근 저장된 장소 미리보기
    suspend fun recentSavedPlaces(size: Int): List<Place>

    // 지난 데이트 일정 미리보기 (연결됐을 때만 값이 있다)
    suspend fun pastDates(size: Int): List<DateSchedule>
}
