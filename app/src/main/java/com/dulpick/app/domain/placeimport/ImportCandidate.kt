package com.dulpick.app.domain.placeimport

import com.dulpick.app.domain.place.PlaceCategory

// 추출된 장소 후보 하나. place 가 있으면 카카오와 매칭된 실장소다 (iOS ImportCandidate 대응)
data class ImportCandidate(
    val candidateId: Long,
    val verificationStatus: VerificationStatus,
    val extractedName: String,
    val extractedAddressHint: String?,
    val place: ImportPlace?,
    val evidence: String?,
)

enum class VerificationStatus {
    EXTRACTED, VERIFIED, REVIEW_REQUIRED
}

// 후보에 매칭된 실제 장소
data class ImportPlace(
    val placeId: Long,
    val kakaoPlaceId: String,
    val name: String,
    val address: String,
    val roadAddress: String,
    val latitude: Double,
    val longitude: Double,
    // 서버 categoryCode/categoryName 을 도메인 카테고리로 매핑한 결과
    val category: PlaceCategory,
    val savedByMe: Boolean,
    val thumbnailUrl: String?,
    val imageUrls: List<String>,
)
