package com.dulpick.app.domain.profile

// 알림 설정. 콘텐츠 저장·데이트 일정·마케팅 수신 여부
data class NotificationSettings(
    val contentSavedEnabled: Boolean,
    val dateScheduleEnabled: Boolean,
    val marketingEnabled: Boolean,
    // 사용자가 동의한 마케팅 약관 버전. 아직 동의 안 했으면 null
    val marketingConsentVersion: String? = null,
    // 현재 동의 가능한 마케팅 약관 버전. 마케팅을 처음 켤 때 이 값을 동의 버전으로 보낸다
    val availableMarketingConsentVersion: String? = null,
)
