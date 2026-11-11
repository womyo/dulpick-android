package com.dulpick.app.feature.course

// 핀 id. 같은 장소에 카테고리 핀과 숫자 핀이 함께 서서 앞붙이로 가른다.
// 같은 id 를 쓰면 지도가 하나를 버린다 (iOS CourseMarkerID 대응)
internal object CourseMarkerId {

    // 핀을 눌렀을 때 무엇을 누른 것인지
    sealed interface Target {
        // 저장한 장소. 이 화면에서 고르거나 뺀다
        data class Candidate(val placeId: String) : Target

        // 기존 코스 장소. 눌러도 아무 일도 없다
        data object Existing : Target
    }

    fun numbered(placeId: String): String = NUMBERED_PREFIX + placeId

    fun existingCategory(placeId: String): String = EXISTING_CATEGORY_PREFIX + placeId

    fun existingNumbered(placeId: String): String = EXISTING_NUMBERED_PREFIX + placeId

    fun targetOf(markerId: String): Target = when {
        markerId.startsWith(EXISTING_NUMBERED_PREFIX) || markerId.startsWith(EXISTING_CATEGORY_PREFIX) ->
            Target.Existing
        // 카테고리 핀은 장소 id 를 그대로 쓴다. 앞붙이가 없으면 그 경우다
        else -> Target.Candidate(markerId.removePrefix(NUMBERED_PREFIX))
    }

    private const val NUMBERED_PREFIX = "numbered:"
    private const val EXISTING_CATEGORY_PREFIX = "existing:"
    private const val EXISTING_NUMBERED_PREFIX = "existing-numbered:"
}
