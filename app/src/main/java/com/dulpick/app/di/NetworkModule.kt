package com.dulpick.app.di

import com.dulpick.app.BuildConfig
import com.dulpick.app.core.network.Authed
import com.dulpick.app.core.network.Plain
import com.dulpick.app.data.auth.local.AuthLocalDataSource
import com.dulpick.app.data.auth.remote.AuthApi
import com.dulpick.app.data.auth.token.AuthTokenAuthenticator
import com.dulpick.app.data.auth.token.AuthTokenInterceptor
import com.dulpick.app.data.auth.token.AuthTokenRefresher
import com.dulpick.app.data.couple.remote.CoupleApi
import com.dulpick.app.data.explore.remote.ExploreApi
import com.dulpick.app.data.profile.remote.ProfileApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun json(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    // 로그인·토큰 재발급이 이 클라이언트를 탄다. 본문에 idToken/refreshToken 이 실려 로거를 아예 붙이지 않는다
    @Provides
    @Singleton
    @Plain
    fun plainOkHttp(): OkHttpClient = OkHttpClient.Builder()
        .build()

    @Provides
    @Singleton
    @Plain
    fun plainRetrofit(@Plain client: OkHttpClient, json: Json): Retrofit = retrofit(client, json)

    @Provides
    @Singleton
    @Plain
    fun plainAuthApi(@Plain retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun authTokenInterceptor(authLocal: AuthLocalDataSource): AuthTokenInterceptor =
        AuthTokenInterceptor(authLocal)

    @Provides
    @Singleton
    fun authTokenRefresher(
        authLocal: AuthLocalDataSource,
        @Plain plainApi: AuthApi,
    ): AuthTokenRefresher = AuthTokenRefresher(authLocal, plainApi)

    @Provides
    @Singleton
    fun authTokenAuthenticator(
        authLocal: AuthLocalDataSource,
        refresher: AuthTokenRefresher,
    ): AuthTokenAuthenticator = AuthTokenAuthenticator(authLocal, refresher)

    @Provides
    @Singleton
    @Authed
    fun authedOkHttp(
        interceptor: AuthTokenInterceptor,
        authenticator: AuthTokenAuthenticator,
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(interceptor)
        .addInterceptor(loggingInterceptor())
        .authenticator(authenticator)
        .build()

    @Provides
    @Singleton
    @Authed
    fun authedRetrofit(@Authed client: OkHttpClient, json: Json): Retrofit = retrofit(client, json)

    @Provides
    @Singleton
    @Authed
    fun authedAuthApi(@Authed retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    @Authed
    fun profileApi(@Authed retrofit: Retrofit): ProfileApi = retrofit.create(ProfileApi::class.java)

    @Provides
    @Singleton
    @Authed
    fun coupleApi(@Authed retrofit: Retrofit): CoupleApi = retrofit.create(CoupleApi::class.java)

    @Provides
    @Singleton
    @Authed
    fun exploreApi(@Authed retrofit: Retrofit): ExploreApi = retrofit.create(ExploreApi::class.java)

    private fun retrofit(client: OkHttpClient, json: Json): Retrofit {
        val raw = BuildConfig.API_BASE_URL.ifEmpty { PLACEHOLDER_BASE_URL }
        val baseUrl = if (raw.endsWith("/")) raw else "$raw/"
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    // Authorization 헤더·인증 본문이 Logcat 에 남지 않도록 BASIC(메서드·URL·상태만) 이하로 제한한다.
    // 혹시 레벨이 올라가도 민감 헤더는 가린다
    private fun loggingInterceptor(): HttpLoggingInterceptor = HttpLoggingInterceptor().apply {
        redactHeader("Authorization")
        redactHeader("Cookie")
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BASIC
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    // API_BASE_URL 이 비었을 때(시크릿 미설정) Retrofit 이 죽지 않게 하는 자리표시자
    private const val PLACEHOLDER_BASE_URL = "https://localhost/"
}
