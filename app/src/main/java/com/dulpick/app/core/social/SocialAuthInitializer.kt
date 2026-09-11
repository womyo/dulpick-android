package com.dulpick.app.core.social

import android.content.Context
import com.kakao.sdk.common.KakaoSdk

// Kakao SDK 초기화. DulpickApp 이 호출한다
object SocialAuthInitializer {
    fun init(context: Context, kakaoNativeAppKey: String) {
        if (kakaoNativeAppKey.isNotEmpty()) {
            KakaoSdk.init(context, kakaoNativeAppKey)
        }
    }
}
