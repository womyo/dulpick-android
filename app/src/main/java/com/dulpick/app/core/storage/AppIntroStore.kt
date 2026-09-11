package com.dulpick.app.core.storage

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

// 앱 인트로를 이미 봤는지 기록한다. 민감 정보가 아니라 일반 SharedPreferences 를 쓴다 (iOS onboardingClient 대응)
interface AppIntroStore {
    suspend fun hasSeenAppIntro(): Boolean
    suspend fun markAppIntroSeen()
}

@Singleton
class DefaultAppIntroStore @Inject constructor(
    @ApplicationContext context: Context,
) : AppIntroStore {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    override suspend fun hasSeenAppIntro(): Boolean = withContext(Dispatchers.IO) {
        prefs.getBoolean(KEY_SEEN, false)
    }

    override suspend fun markAppIntroSeen(): Unit = withContext(Dispatchers.IO) {
        prefs.edit().putBoolean(KEY_SEEN, true).apply()
    }

    private companion object {
        const val FILE_NAME = "dulpick_onboarding"
        const val KEY_SEEN = "has_seen_app_intro"
    }
}
