package com.dulpick.app.domain.couple

// 내 초대 코드. shareUrl 이 있으면 공유 링크로, 없으면 코드 문자열로 공유한다
data class InviteCode(
    val value: String,
    val shareUrl: String?,
)

// 커플 연결 완료 시 상대 정보
data class Couple(
    val partnerNickname: String,
    val partnerIconId: Int,
)

// 커플 상태 조회의 me/partner 공용 프로필
data class CoupleMember(
    val nickname: String,
    val iconId: Int,
)

// 커플 연결 상태 조회(GET /couples/me) 결과. connected 로 연결 여부를 직접 판단한다
data class CoupleStatus(
    val connected: Boolean,
    val me: CoupleMember,
    val partner: CoupleMember?,
    val daysTogether: Int?,
)
