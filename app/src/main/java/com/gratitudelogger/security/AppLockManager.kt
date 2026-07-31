package com.gratitudelogger.security

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Locks the app the instant it leaves the foreground (onStop), regardless of back-stack
 * state. Starts locked so a cold start always requires authentication.
 */
@Singleton
class AppLockManager @Inject constructor() : DefaultLifecycleObserver {

    private val _isLocked = MutableStateFlow(true)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStop(owner: LifecycleOwner) {
        _isLocked.value = true
    }

    fun unlock() {
        _isLocked.value = false
    }
}
