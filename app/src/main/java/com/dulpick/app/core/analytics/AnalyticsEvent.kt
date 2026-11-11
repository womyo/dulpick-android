package com.dulpick.app.core.analytics

// 코스 만들기와 코스 보기를 어디서 눌렀는지
enum class AnalyticsEntryPoint(val value: String) {
    HOME_BANNER("home_banner"),
    MAP_FLOATING_BUTTON("map_floating_button"),
}

// 장소를 밖에서 공유받아 저장했는지, 앱 안에서 저장했는지
enum class AnalyticsSaveSource(val value: String) {
    SHARE("share"),
    IN_APP("in_app"),
}

// 믹스패널에 보내는 이벤트. 이름은 기획 표와 글자 그대로 같아야 하고 iOS 와도 같다.
//
// 한 번 쌓인 이름은 못 바꾼다 — 오타는 에러가 아니라 새 이벤트가 된다 (iOS AnalyticsEvent 대응)
sealed class AnalyticsEvent(val name: String, val properties: Map<String, Any> = emptyMap()) {
    data object AppOpened : AnalyticsEvent("app_opened")
    data object LoginStarted : AnalyticsEvent("login_started")
    data object CoupleConnectStarted : AnalyticsEvent("couple_connect_started")
    data object CoupleConnected : AnalyticsEvent("couple_connected")
    data object ShareImportStarted : AnalyticsEvent("share_import_started")
    data object ExploreViewed : AnalyticsEvent("explore_viewed")
    data object SavedPlaceDetailViewed : AnalyticsEvent("saved_place_detail_viewed")
    data object MapViewed : AnalyticsEvent("map_viewed")
    data object PlaceAddedToCourse : AnalyticsEvent("place_added_to_course")
    data object CourseCreated : AnalyticsEvent("course_created")
    data object CourseEditStarted : AnalyticsEvent("course_edit_started")
    data object CourseEdited : AnalyticsEvent("course_edited")
    data object MyPageViewed : AnalyticsEvent("mypage_viewed")
    data object PreferenceSaved : AnalyticsEvent("preference_saved")
    data object PlaceSaveModalViewed : AnalyticsEvent("place_save_modal_viewed")
    data object CourseScheduleCompleted : AnalyticsEvent("course_schedule_completed")
    data object PreferenceSetupStarted : AnalyticsEvent("preference_setup_started")

    class CourseCreateStarted(entryPoint: AnalyticsEntryPoint) :
        AnalyticsEvent("course_create_started", mapOf(ENTRY_POINT to entryPoint.value))

    class CourseViewed(entryPoint: AnalyticsEntryPoint) :
        AnalyticsEvent("course_viewed", mapOf(ENTRY_POINT to entryPoint.value))

    // userId 가 없으면 키 자체를 싣지 않는다 (iOS 와 같다)
    class CourseAlarmStarted(userId: String?) :
        AnalyticsEvent("course_alarm_started", userId?.let { mapOf(USER_ID to it) }.orEmpty())

    class PlaceSaveStarted(saveSource: AnalyticsSaveSource) :
        AnalyticsEvent("place_save_started", mapOf(SAVE_SOURCE to saveSource.value))

    class PlaceSaveCompleted(saveSource: AnalyticsSaveSource, userId: String?) :
        AnalyticsEvent(
            "place_save_completed",
            mapOf(SAVE_SOURCE to saveSource.value) + userId?.let { mapOf(USER_ID to it) }.orEmpty(),
        )

    companion object {
        // 진행 중인 코스가 있으면 보기, 없으면 만들기 (iOS courseButtonTapped·bannerTapped 규칙)
        fun courseButton(entryPoint: AnalyticsEntryPoint, hasCourse: Boolean): AnalyticsEvent =
            if (hasCourse) CourseViewed(entryPoint) else CourseCreateStarted(entryPoint)

        private const val ENTRY_POINT = "entry_point"
        private const val SAVE_SOURCE = "save_source"
        private const val USER_ID = "user_id"
    }
}
