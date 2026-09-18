package com.dulpick.app.domain.placeimport

// 공유 링크로 장소 추출을 시작하고, 진행 상태를 폴링하고, 고른 후보를 저장한다 (iOS PlaceImportClient 대응)
interface PlaceImportRepository {
    // 링크로 추출 시작. 반환된 importId 로 이후 폴링한다
    suspend fun start(sourceUrl: String): PlaceImport

    // 진행 상태 조회. nextAction 이 WAIT 면 retryAfterSeconds 뒤에 다시 부른다
    suspend fun poll(importId: Long): PlaceImport

    // 고른 후보들을 저장 확정
    suspend fun confirm(importId: Long, candidateIds: List<Long>)
}
