package com.dulpick.app.core.map

import android.content.Context
import com.kakao.vectormap.KakaoMapSdk

// 카카오 지도 SDK 초기화. SDK 를 아는 것은 이 모듈뿐이고 DulpickApp 이 호출한다 (iOS KakaoMapBootstrap 대응)
object KakaoMapInitializer {
    fun init(context: Context, kakaoNativeAppKey: String) {
        if (kakaoNativeAppKey.isNotEmpty()) {
            KakaoMapSdk.init(context, kakaoNativeAppKey)
        }
    }
}
