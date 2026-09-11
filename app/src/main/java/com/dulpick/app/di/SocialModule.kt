package com.dulpick.app.di

import com.dulpick.app.BuildConfig
import com.dulpick.app.core.social.ActivityProvider
import com.dulpick.app.core.social.CurrentActivityProvider
import com.dulpick.app.core.social.GoogleAuthClient
import com.dulpick.app.core.social.GoogleClient
import com.dulpick.app.core.social.KakaoAuthClient
import com.dulpick.app.core.social.KakaoClient
import com.dulpick.app.core.social.SocialAuthClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SocialModule {

    @Provides
    @Singleton
    fun currentActivityProvider(): CurrentActivityProvider = CurrentActivityProvider()

    @Provides
    fun activityProvider(provider: CurrentActivityProvider): ActivityProvider = provider

    @Provides
    @Singleton
    @KakaoClient
    fun kakaoClient(activityProvider: ActivityProvider): SocialAuthClient =
        KakaoAuthClient(activityProvider)

    @Provides
    @Singleton
    @GoogleClient
    fun googleClient(activityProvider: ActivityProvider): SocialAuthClient =
        GoogleAuthClient(activityProvider, BuildConfig.GOOGLE_WEB_CLIENT_ID)
}
