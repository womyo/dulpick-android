package com.dulpick.app

import android.app.Application
import com.dulpick.app.core.social.CurrentActivityProvider
import com.dulpick.app.core.social.SocialAuthInitializer
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class DulpickApp : Application() {

    @Inject
    lateinit var activityProvider: CurrentActivityProvider

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(activityProvider)
        SocialAuthInitializer.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)
    }
}
