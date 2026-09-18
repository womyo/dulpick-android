package com.dulpick.app.data.placeimport.mapper

import com.dulpick.app.data.placeimport.remote.dto.ImportCandidateDto
import com.dulpick.app.data.placeimport.remote.dto.ImportContentDto
import com.dulpick.app.data.placeimport.remote.dto.ImportPlaceDto
import com.dulpick.app.data.placeimport.remote.dto.PlaceImportConfirmRequestDto
import com.dulpick.app.data.place.mapper.PlaceCategoryMapper
import com.dulpick.app.data.placeimport.remote.dto.PlaceImportResponseDto
import com.dulpick.app.data.placeimport.remote.dto.PlaceImportStartRequestDto
import com.dulpick.app.domain.placeimport.ImportAuthor
import com.dulpick.app.domain.placeimport.ImportCandidate
import com.dulpick.app.domain.placeimport.ImportContent
import com.dulpick.app.domain.placeimport.ImportFailure
import com.dulpick.app.domain.placeimport.ImportNextAction
import com.dulpick.app.domain.placeimport.ImportPlace
import com.dulpick.app.domain.placeimport.ImportSourceType
import com.dulpick.app.domain.placeimport.ImportStatus
import com.dulpick.app.domain.placeimport.PlaceImport
import com.dulpick.app.domain.placeimport.VerificationStatus

object PlaceImportDtoMapper {

    fun toStartRequest(sourceUrl: String): PlaceImportStartRequestDto =
        PlaceImportStartRequestDto(sourceUrl = sourceUrl)

    fun toConfirmRequest(candidateIds: List<Long>): PlaceImportConfirmRequestDto =
        PlaceImportConfirmRequestDto(
            selections = candidateIds.map { PlaceImportConfirmRequestDto.SelectionDto(candidateId = it) },
        )

    fun toDomain(dto: PlaceImportResponseDto): PlaceImport = PlaceImport(
        importId = dto.importId,
        contentId = dto.contentId,
        canonicalUrl = dto.canonicalUrl,
        sourceType = enumOrDefault(dto.sourceType, ImportSourceType.INSTAGRAM_POST),
        status = enumOrDefault(dto.status, ImportStatus.FAILED),
        // 서버 enum 이 확장돼 모르는 값이 와도 RETRY(=실패)로 끊지 않고 상태 기반 처리(NONE)로 넘긴다
        nextAction = enumOrDefault(dto.nextAction, ImportNextAction.NONE),
        retryAfterSeconds = dto.retryAfterSeconds,
        failure = dto.failure?.let { ImportFailure(code = it.code, retryable = it.retryable) },
        content = toDomain(dto.content),
        candidates = dto.candidates.map(::toDomain),
    )

    private fun toDomain(dto: ImportContentDto): ImportContent = ImportContent(
        title = dto.title,
        caption = dto.caption,
        thumbnailUrl = dto.thumbnailUrl,
        author = dto.author?.let { ImportAuthor(displayName = it.displayName, username = it.username) },
        publishedOn = dto.publishedOn,
    )

    private fun toDomain(dto: ImportCandidateDto): ImportCandidate = ImportCandidate(
        candidateId = dto.candidateId,
        verificationStatus = enumOrDefault(dto.verificationStatus, VerificationStatus.EXTRACTED),
        extractedName = dto.extractedName,
        extractedAddressHint = dto.extractedAddressHint,
        place = dto.place?.let(::toDomain),
        evidence = dto.evidence,
    )

    private fun toDomain(dto: ImportPlaceDto): ImportPlace = ImportPlace(
        placeId = dto.placeId,
        kakaoPlaceId = dto.kakaoPlaceId,
        name = dto.name,
        address = dto.address,
        roadAddress = dto.roadAddress,
        latitude = dto.latitude,
        longitude = dto.longitude,
        category = PlaceCategoryMapper.fromCodeOrName(dto.category, dto.categoryName),
        savedByMe = dto.savedByMe,
        thumbnailUrl = dto.thumbnailUrl,
        imageUrls = dto.imageUrls,
    )

    private inline fun <reified T : Enum<T>> enumOrDefault(raw: String, default: T): T =
        enumValues<T>().firstOrNull { it.name == raw } ?: default
}
