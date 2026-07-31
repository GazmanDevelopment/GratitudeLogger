package com.gratitudelogger

import android.app.Application
import com.gratitudelogger.security.AppLockManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class GratitudeLoggerApp : Application() {

    // Injected (not just referenced) so Hilt constructs it - and registers its
    // ProcessLifecycleOwner observer - during app startup rather than on first UI use.
    @Inject
    lateinit var appLockManager: AppLockManager
}
