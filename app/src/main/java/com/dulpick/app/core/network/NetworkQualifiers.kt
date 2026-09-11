package com.dulpick.app.core.network

import javax.inject.Qualifier

// 토큰 없이 부르는 클라이언트 (nonce, social-login, reissue)
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Plain

// Bearer 토큰 + 401 시 재발급을 붙인 클라이언트
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Authed
