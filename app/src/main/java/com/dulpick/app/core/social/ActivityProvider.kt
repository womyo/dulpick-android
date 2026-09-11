package com.dulpick.app.core.social

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton

// 현재 화면에 뜬 Activity 를 소셜 SDK 에 넘긴다
interface ActivityProvider {
    val currentActivity: Activity?
}

// Application.ActivityLifecycleCallbacks 로 resumed Activity 를 추적한다
@Singleton
class CurrentActivityProvider @Inject constructor() :
    ActivityProvider, Application.ActivityLifecycleCallbacks {

    private var activityRef: WeakReference<Activity>? = null

    override val currentActivity: Activity?
        get() = activityRef?.get()

    override fun onActivityResumed(activity: Activity) {
        activityRef = WeakReference(activity)
    }

    override fun onActivityPaused(activity: Activity) {
        if (activityRef?.get() === activity) {
            activityRef = null
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
