package com.dulpick.app.core.social

// Data 에서 AuthError 로 매핑된다
sealed class SocialAuthException : Exception() {
    data object Cancelled : SocialAuthException()
    data object Failed : SocialAuthException()
}
