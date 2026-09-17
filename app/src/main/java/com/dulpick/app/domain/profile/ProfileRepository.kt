package com.dulpick.app.domain.profile

// Feature 는 이 인터페이스로만 프로필 데이터에 접근한다
interface ProfileRepository {
    // 현재 회원 프로필 조회(닉네임·아이콘·성향)
    suspend fun profile(): UserProfile

    // 회원 탈퇴
    suspend fun withdraw()

    // 온보딩 여부에 따라 초기화(POST) 또는 수정(PATCH) 한다
    suspend fun updateNickname(nickname: String, iconId: Int): UserProfile

    // 프로필 수정(PATCH). 이미 온보딩된 회원의 닉네임·아이콘을 바로 바꾼다
    suspend fun updateProfile(nickname: String, iconId: Int): UserProfile

    // 성향 4축 저장(PUT) 후 최신 회원 정보를 돌려준다
    suspend fun updateDatePreference(preference: DatePreference): UserProfile

    // 알림 설정 조회
    suspend fun notificationSettings(): NotificationSettings

    // 알림 설정 갱신(전체 교체)
    suspend fun updateNotificationSettings(settings: NotificationSettings): NotificationSettings
}
