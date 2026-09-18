package com.dulpick.app.domain.profile

// 4축이 모두 채워졌을 때만 만들어진다. 부분 선택은 저장 대상이 아니다
data class DatePreference(
    val indoorOutdoor: IndoorOutdoor,
    val activityLevel: ActivityLevel,
    val dateTime: DateTime,
    val dateFocus: DateFocus,
)

// 서버 문자열 계약(값·변환)은 도메인에 두지 않는다. 문자열 ↔ enum 변환은 data 의 ProfileDtoMapper 가 맡는다
enum class IndoorOutdoor { INDOOR, OUTDOOR }

enum class ActivityLevel { ACTIVE, STATIC }

enum class DateTime { DAY, NIGHT }

enum class DateFocus { FOOD, SIGHTSEEING }
