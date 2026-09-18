package com.dulpick.app.data.placeimport.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class PlaceImportStartRequestDto(
    val sourceUrl: String,
)

@Serializable
data class PlaceImportConfirmRequestDto(
    val selections: List<SelectionDto>,
) {
    @Serializable
    data class SelectionDto(
        val candidateId: Long,
        // 사용자 지정 별칭. 지금은 항상 null
        val alias: String? = null,
    )
}

@Serializable
data class PlaceImportResponseDto(
    val importId: Long = 0,
    val contentId: Long? = null,
    val canonicalUrl: String = "",
    val sourceType: String = "",
    val status: String = "",
    val nextAction: String = "",
    val retryAfterSeconds: Int? = null,
    val failure: ImportFailureDto? = null,
    val content: ImportContentDto = ImportContentDto(),
    val candidates: List<ImportCandidateDto> = emptyList(),
)

@Serializable
data class ImportFailureDto(
    val code: String = "",
    val retryable: Boolean = false,
)

@Serializable
data class ImportContentDto(
    val title: String? = null,
    val caption: String? = null,
    val thumbnailUrl: String? = null,
    val author: ImportAuthorDto? = null,
    val publishedOn: String? = null,
)

@Serializable
data class ImportAuthorDto(
    val displayName: String = "",
    val username: String = "",
)

@Serializable
data class ImportCandidateDto(
    val candidateId: Long = 0,
    val verificationStatus: String = "",
    val extractedName: String = "",
    val extractedAddressHint: String? = null,
    val place: ImportPlaceDto? = null,
    val evidence: String? = null,
)

@Serializable
data class ImportPlaceDto(
    val placeId: Long = 0,
    val kakaoPlaceId: String = "",
    val name: String = "",
    val address: String = "",
    val roadAddress: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val category: String = "",
    val categoryName: String = "",
    val savedByMe: Boolean = false,
    val thumbnailUrl: String? = null,
    val imageUrls: List<String> = emptyList(),
)
