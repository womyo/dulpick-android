package com.dulpick.app.core.social

import javax.inject.Qualifier

// nonce 를 SDK 에 넣어 idToken 을 받는다
interface SocialAuthClient {
    suspend fun login(nonce: String): SocialAuthCredential
}

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class KakaoClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GoogleClient

// 구글 웹 클라이언트 ID(BuildConfig 값)를 주입 대상과 구분하는 한정자
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GoogleWebClientId
