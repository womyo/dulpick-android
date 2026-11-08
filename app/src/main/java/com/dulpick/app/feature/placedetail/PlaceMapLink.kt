package com.dulpick.app.feature.placedetail

import java.net.URLEncoder

// 장소 상세 지도 시트에서 고르는 지도 앱. 시트에 보이는 차례다
enum class PlaceMapApp { NAVER, KAKAO }

// 지도 앱 하나를 여는 주소 한 쌍. 앱 주소를 먼저 열고, 그 앱이 없으면 웹 주소를 연다
data class PlaceMapLink(val appUrl: String, val webUrl: String) {

    companion object {
        // name 은 검색어로 쓸 실제 장소 이름이다. 별칭을 넘기지 않는다.
        // 카카오는 장소 번호가 있으면 장소 화면을, 없으면 이름 검색을 연다
        fun of(
            app: PlaceMapApp,
            name: String,
            kakaoPlaceId: String?,
            kakaoPlaceUrl: String?,
            packageName: String,
        ): PlaceMapLink {
            val query = encode(name)
            return when (app) {
                PlaceMapApp.NAVER -> PlaceMapLink(
                    // 네이버는 appname 으로 돌아갈 앱을 알아야 주소를 받는다
                    appUrl = "nmap://search?query=$query&appname=${encode(packageName)}",
                    webUrl = "https://m.map.naver.com/search?query=$query",
                )
                PlaceMapApp.KAKAO -> kakaoLink(query, kakaoPlaceId, kakaoPlaceUrl)
            }
        }

        private fun kakaoLink(query: String, kakaoPlaceId: String?, kakaoPlaceUrl: String?): PlaceMapLink =
            if (kakaoPlaceId != null) {
                PlaceMapLink(
                    appUrl = "kakaomap://place?id=$kakaoPlaceId",
                    webUrl = kakaoPlaceUrl ?: "https://place.map.kakao.com/$kakaoPlaceId",
                )
            } else {
                PlaceMapLink(
                    appUrl = "kakaomap://search?q=$query",
                    webUrl = "https://m.map.kakao.com/actions/searchView?q=$query",
                )
            }

        // URLEncoder 는 공백을 + 로 바꿔 받는 쪽이 그대로 읽는다. %20 으로 되돌린다
        private fun encode(text: String): String =
            URLEncoder.encode(text, "UTF-8").replace("+", "%20")
    }
}
