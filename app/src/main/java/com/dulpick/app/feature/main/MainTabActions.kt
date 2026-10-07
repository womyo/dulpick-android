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
    // 지도 좌하단 코스 버튼·홈 배너. 진행 중인 코스가 있으면 결과로, 없으면 날짜 고르기로
    val onOpenCourse: (dateCourseId: String?) -> Unit,
    // 지난 데이트 코스 보기. 결과 화면에서 수정·알리기를 숨긴다
    val onOpenPastCourse: (dateCourseId: String) -> Unit,
    // 마이페이지 → 공지사항 목록
    val onOpenNotice: () -> Unit,
)
