package com.gallery.cleaner.util

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.gallery.cleaner.util.log.AppLogger
import java.lang.ref.WeakReference

object ActivityProvider : Application.ActivityLifecycleCallbacks {
    private const val TAG = "ActivityProvider"
    private var currentActivityRef: WeakReference<Activity>? = null

    val currentActivity: Activity?
        get() = currentActivityRef?.get()

    fun init(application: Application) {
        application.registerActivityLifecycleCallbacks(this)
        AppLogger.d(TAG, "ActivityProvider 已初始化")
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        currentActivityRef = WeakReference(activity)
        AppLogger.d(TAG, "Activity 创建: ${activity.javaClass.simpleName}")
    }

    override fun onActivityStarted(activity: Activity) {
        currentActivityRef = WeakReference(activity)
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivityRef = WeakReference(activity)
    }

    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivityRef?.get() == activity) {
            currentActivityRef = null
            AppLogger.d(TAG, "Activity 销毁: ${activity.javaClass.simpleName}")
        }
    }
}
