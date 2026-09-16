package com.dulpick.app.data.couple.mapper

import com.dulpick.app.data.couple.remote.dto.ConnectionCodeRequestDto
import com.dulpick.app.data.couple.remote.dto.ConnectionCodeResponseDto
import com.dulpick.app.data.couple.remote.dto.CoupleConnectionStatusResponseDto
import com.dulpick.app.data.couple.remote.dto.CoupleMemberProfileResponseDto
import com.dulpick.app.domain.couple.Couple
import com.dulpick.app.domain.couple.CoupleError
import com.dulpick.app.domain.couple.CoupleMember
import com.dulpick.app.domain.couple.CoupleStatus
import com.dulpick.app.domain.couple.InviteCode

object CoupleDtoMapper {
    // 연결 코드를 서버 발급 형식(공백 제거·대문자)으로 맞춘다. 정규화는 여기 한 곳에서만 한다
    fun toRequest(inviteCode: String): ConnectionCodeRequestDto =
        ConnectionCodeRequestDto(connectionCode = inviteCode.trim().uppercase())

    fun toDomain(dto: ConnectionCodeResponseDto): InviteCode =
        InviteCode(value = dto.code, shareUrl = dto.shareUrl)

    fun toDomain(dto: CoupleConnectionStatusResponseDto): Couple? {
        if (!dto.connected) return null
        val partner = dto.partner ?: return null
        return Couple(partnerNickname = partner.nickname, partnerIconId = partner.profileIcon)
    }

    // me 가 없거나 connected 인데 partner 가 없으면 불완전 응답이라 에러로 올린다
    fun toStatus(dto: CoupleConnectionStatusResponseDto): CoupleStatus {
        val me = dto.me?.toMember() ?: throw CoupleError.Unknown
        val partner = dto.partner?.toMember()
        if (dto.connected && partner == null) throw CoupleError.Unknown
        return CoupleStatus(
            connected = dto.connected,
            me = me,
            partner = if (dto.connected) partner else null,
            daysTogether = dto.daysTogether,
        )
    }

    private fun CoupleMemberProfileResponseDto.toMember(): CoupleMember =
        CoupleMember(nickname = nickname, iconId = profileIcon)
}
