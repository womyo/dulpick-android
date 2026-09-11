package com.dulpick.app.data.auth.social

import com.dulpick.app.core.social.GoogleClient
import com.dulpick.app.core.social.KakaoClient
import com.dulpick.app.core.social.SocialAuthClient
import com.dulpick.app.core.social.SocialAuthCredential
import com.dulpick.app.domain.auth.AuthProvider
import javax.inject.Inject

// provider 로 SDK 클라이언트를 고른다
// 에러(SocialAuthException)는 그대로 올리고 AuthErrorMapper 가 매핑한다
class SocialAuthProvider @Inject constructor(
    @KakaoClient private val kakao: SocialAuthClient,
    @GoogleClient private val google: SocialAuthClient,
) {
    suspend fun login(provider: AuthProvider, nonce: String): SocialAuthCredential =
        client(provider).login(nonce)

    private fun client(provider: AuthProvider): SocialAuthClient = when (provider) {
        AuthProvider.KAKAO -> kakao
        AuthProvider.GOOGLE -> google
    }
}
