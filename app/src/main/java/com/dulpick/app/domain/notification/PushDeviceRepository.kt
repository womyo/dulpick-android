package com.dulpick.app.domain.notification

// 푸시 기기 등록 계약 (iOS NotificationClient 의 registerDevice·unregisterDevice 대응).
// 알림 수신 여부(콘텐츠 저장·데이트 일정·마케팅) 설정은 ProfileRepository 가 갖는다
interface PushDeviceRepository {
    // 인자는 FCM 등록 토큰. 같은 기기를 다시 불러도 덮어쓴다
    suspend fun registerDevice(token: String)

    // 로그아웃 경로가 부른다. 이미 해제된 기기를 또 불러도 성공으로 본다
    suspend fun unregisterDevice()
}
