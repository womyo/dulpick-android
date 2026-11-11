package com.dulpick.app.feature.course

import android.net.Uri
import com.dulpick.app.domain.course.CourseTravelMode
import com.dulpick.app.domain.place.Place

// 구간 하나의 카카오맵 길찾기 주소. 앱을 먼저 열고, 앱이 없으면 웹을 연다
data class CourseDirections(val appUrl: String, val webUrl: String) {

    companion object {
        fun of(origin: Place, destination: Place, mode: CourseTravelMode): CourseDirections {
            val appMode = if (mode == CourseTravelMode.WALKING) "foot" else "publictransit"
            val webMode = if (mode == CourseTravelMode.WALKING) "walk" else "traffic"
            val start = origin.coordinate
            val end = destination.coordinate
            return CourseDirections(
                appUrl = "kakaomap://route" +
                    "?sp=${start.latitude},${start.longitude}" +
                    "&ep=${end.latitude},${end.longitude}" +
                    "&by=$appMode",
                webUrl = "https://map.kakao.com/link/by/$webMode/" +
                    "${webPoint(origin)}/${webPoint(destination)}",
            )
        }

        // 이름,위도,경도. 쉼표·슬래시는 웹 주소의 구분자라 이름에서 공백으로 바꾼다
        private fun webPoint(place: Place): String {
            val name = place.name.replace(',', ' ').replace('/', ' ')
            return "${Uri.encode(name)},${place.coordinate.latitude},${place.coordinate.longitude}"
        }
    }
}
