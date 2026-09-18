package com.dulpick.app.domain.placeimport

// 인스타 공유 링크로 시작한 장소 추출 세션 (iOS PlaceImport 대응)
data class PlaceImport(
    val importId: Long,
    val contentId: Long?,
    val canonicalUrl: String,
    val sourceType: ImportSourceType,
    val status: ImportStatus,
    val nextAction: ImportNextAction,
    // 폴링 재요청까지 권장 대기 초. WAIT 상태에서 이 간격으로 다시 poll 한다
    val retryAfterSeconds: Int?,
    val failure: ImportFailure?,
    val content: ImportContent,
    val candidates: List<ImportCandidate>,
)

enum class ImportStatus {
    RECEIVED, PROCESSING, REVIEW_REQUIRED, COMPLETED, FAILED
}

// 서버가 알려주는 다음 행동. 화면 흐름(대기·선택·재시도·완료)을 이걸로 가른다
enum class ImportNextAction {
    WAIT, SELECT_PLACES, RETRY, COMPLETED, NONE
}

enum class ImportSourceType {
    INSTAGRAM_REEL, INSTAGRAM_POST
}

data class ImportFailure(
    val code: String,
    val retryable: Boolean,
)
