package com.dulpick.app.di

import com.dulpick.app.BuildConfig
import com.dulpick.app.core.social.ActivityProvider
import com.dulpick.app.core.social.CurrentActivityProvider
import com.dulpick.app.core.social.GoogleAuthClient
import com.dulpick.app.core.social.GoogleClient
import com.dulpick.app.core.social.GoogleWebClientId
import com.dulpick.app.core.social.KakaoAuthClient
import com.dulpick.app.core.social.KakaoClient
import com.dulpick.app.core.social.SocialAuthClient
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

// 계약↔구현은 @Binds, 외부 값(BuildConfig)은 @Provides
@Module
@InstallIn(SingletonComponent::class)
abstract class SocialModule {

    @Binds
    abstract fun activityProvider(impl: CurrentActivityProvider): ActivityProvider

    @Binds
    @KakaoClient
    abstract fun kakaoClient(impl: KakaoAuthClient): SocialAuthClient

    @Binds
    @GoogleClient
    abstract fun googleClient(impl: GoogleAuthClient): SocialAuthClient

    companion object {
        @Provides
        @GoogleWebClientId
        fun googleWebClientId(): String = BuildConfig.GOOGLE_WEB_CLIENT_ID
    }
}
