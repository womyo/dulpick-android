package com.dulpick.app.domain.couple

// Feature 는 이 인터페이스로만 커플 데이터에 접근한다
interface CoupleRepository {
    // 내 초대 코드 발급/조회
    suspend fun inviteCode(): InviteCode

    // 상대 코드로 연결. 성공하면 상대 정보를 돌려준다
    suspend fun connect(inviteCode: String): Couple

    // 현재 연결 상태. 미연결이면 null
    suspend fun current(): CoupleStatus?

    // 커플 연결 끊기
    suspend fun disconnect()
}
