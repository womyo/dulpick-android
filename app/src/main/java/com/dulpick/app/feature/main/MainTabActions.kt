package com.dulpick.app.feature.main

// 탭 밖(루트)으로 위임하는 화면 이동 묶음
data class MainTabActions(
    val onOpenDateType: () -> Unit,
    val onOpenConnection: () -> Unit,
    // 마이페이지에서 연결 → 완료 시 연결 관리로
    val onOpenCoupleConnect: (myNickname: String) -> Unit,
    // 홈에서 연결 → 완료 시 홈으로 되돌아와 갱신
    val onOpenCoupleConnectFromHome: (myNickname: String) -> Unit,
    // 지도 검색바 → 지도 전용 장소 검색
    val onOpenMapSearch: () -> Unit,
    val onOpenPastDates: (hasCurrentCourse: Boolean) -> Unit,
)
