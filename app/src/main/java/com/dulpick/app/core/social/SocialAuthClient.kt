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
