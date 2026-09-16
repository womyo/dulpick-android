package com.dulpick.app.domain.profile

// 4축이 모두 채워졌을 때만 만들어진다. 부분 선택은 저장 대상이 아니다
data class DatePreference(
    val indoorOutdoor: IndoorOutdoor,
    val activityLevel: ActivityLevel,
    val dateTime: DateTime,
    val dateFocus: DateFocus,
)

// rawValue 는 서버 계약값. DTO 매핑에 그대로 쓴다
enum class IndoorOutdoor(val rawValue: String) {
    INDOOR("INDOOR"),
    OUTDOOR("OUTDOOR"),
    ;

    companion object {
        fun from(raw: String?): IndoorOutdoor? = entries.firstOrNull { it.rawValue == raw }
    }
}

enum class ActivityLevel(val rawValue: String) {
    ACTIVE("ACTIVE"),
    STATIC("STATIC"),
    ;

    companion object {
        fun from(raw: String?): ActivityLevel? = entries.firstOrNull { it.rawValue == raw }
    }
}

enum class DateTime(val rawValue: String) {
    DAY("DAY"),
    NIGHT("NIGHT"),
    ;

    companion object {
        fun from(raw: String?): DateTime? = entries.firstOrNull { it.rawValue == raw }
    }
}

enum class DateFocus(val rawValue: String) {
    FOOD("FOOD"),
    SIGHTSEEING("SIGHTSEEING"),
    ;

    companion object {
        fun from(raw: String?): DateFocus? = entries.firstOrNull { it.rawValue == raw }
    }
}
