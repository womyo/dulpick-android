package com.dulpick.app.core.analytics

import android.util.Log
import com.microsoft.clarity.Clarity

// 대시보드에 남길 화면 이름. 같은 화면은 어디서 들어왔든 한 이름이다 (iOS AnalyticsScreenName 대응)
enum class AnalyticsScreenName(val value: String) {
    SPLASH("Splash"),
    APP_INTRO("AppIntro"),
    AUTH("Auth"),
    NICKNAME("Nickname"),
    COUPLE_CONNECT("CoupleConnect"),
    COUPLE_CODE_INPUT("CoupleCodeInput"),
    COUPLE_COMPLETE("CoupleComplete"),
    DATE_TYPE("DateType"),
    HOME("Home"),
    EXPLORE("Explore"),
    MAP("Map"),
    MY_PAGE("MyPage"),
    SEARCH("Search"),
    PAST_DATE_COURSES("PastDateCourses"),
    COURSE_DATE("CourseDate"),
    COURSE_PLACE_PICK("CoursePlacePick"),
    COURSE_RESULT("CourseResult"),
    COURSE_EDIT("CourseEdit"),
    PLACE_SEARCH("PlaceSearch"),
    PLACE_DETAIL("PlaceDetail"),
    POST_DETAIL("PostDetail"),
    PLACE_ALIAS("PlaceAlias"),
    PLACE_IMPORT("PlaceImport"),
    CONNECTION_MANAGE("ConnectionManage"),
    PROFILE_EDIT("ProfileEdit"),
    WITHDRAW("Withdraw"),
    NOTICE_LIST("NoticeList"),
    NOTICE_DETAIL("NoticeDetail"),
}

// 클라리티에 화면 이름을 알린다.
//
// SDK 의 화면 이름은 전역 값이라 명시로 바꾸기 전까지 유지된다.
// 그래서 화면이 바뀔 때마다 반드시 다시 불러야 한다 (iOS AnalyticsScreen 대응)
object AnalyticsScreen {
    // SDK 는 비어 있지 않고 255자 이하인 이름만 받는다. 어긋나면 이전 이름이 그대로 남는다.
    // 메인 스레드에서 부른다
    fun set(screen: AnalyticsScreenName) {
        if (!Clarity.setCurrentScreenName(screen.value)) {
            Log.e(LOG_TAG, "클라리티 화면 이름 설정에 실패했다 name=${screen.value}")
        }
    }

    private const val LOG_TAG = "Analytics"
}
