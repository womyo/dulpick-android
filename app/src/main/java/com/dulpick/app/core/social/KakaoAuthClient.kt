package com.dulpick.app.core.social

import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import javax.inject.Inject
import javax.inject.Singleton

// 카카오톡 앱이 있으면 앱 로그인, 없으면 계정 로그인
@Singleton
class KakaoAuthClient @Inject constructor(
    private val activityProvider: ActivityProvider,
) : SocialAuthClient {

    override suspend fun login(nonce: String): SocialAuthCredential = withContext(Dispatchers.Main) {
        val activity = activityProvider.currentActivity ?: throw SocialAuthException.Failed

        suspendCancellableCoroutine { continuation ->
            val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
                when {
                    error != null -> continuation.resumeWithException(mapError(error))
                    token?.idToken.isNullOrEmpty() ->
                        continuation.resumeWithException(SocialAuthException.Failed)
                    else -> continuation.resume(SocialAuthCredential(idToken = token.idToken.orEmpty()))
                }
            }

            if (UserApiClient.instance.isKakaoTalkLoginAvailable(activity)) {
                UserApiClient.instance.loginWithKakaoTalk(activity, nonce = nonce, callback = callback)
            } else {
                UserApiClient.instance.loginWithKakaoAccount(activity, nonce = nonce, callback = callback)
            }
        }
    }

    private fun mapError(error: Throwable): SocialAuthException {
        val cancelled = error is ClientError && error.reason == ClientErrorCause.Cancelled
        return if (cancelled) SocialAuthException.Cancelled else SocialAuthException.Failed
    }
}
