package com.dulpick.app.core.social

import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import javax.inject.Inject
import javax.inject.Singleton

// Credential Manager 로 웹 클라이언트 ID audience 의 idToken 을 받는다
@Singleton
class GoogleAuthClient @Inject constructor(
    private val activityProvider: ActivityProvider,
    @GoogleWebClientId private val webClientId: String,
) : SocialAuthClient {

    override suspend fun login(nonce: String): SocialAuthCredential {
        val activity = activityProvider.currentActivity ?: throw SocialAuthException.Failed

        val option = GetSignInWithGoogleOption.Builder(webClientId)
            .setNonce(nonce)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()

        return try {
            val response = CredentialManager.create(activity).getCredential(activity, request)
            val credential = response.credential
            val isGoogleIdToken = credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            if (!isGoogleIdToken) throw SocialAuthException.Failed

            val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
            SocialAuthCredential(idToken = googleCredential.idToken)
        } catch (error: GetCredentialCancellationException) {
            throw SocialAuthException.Cancelled
        } catch (error: GetCredentialException) {
            throw SocialAuthException.Failed
        }
    }
}
