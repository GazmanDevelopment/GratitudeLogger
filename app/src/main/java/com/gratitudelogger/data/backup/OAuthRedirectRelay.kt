package com.gratitudelogger.data.backup

import android.net.Uri
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Routes an OAuth redirect Uri (arriving via MainActivity.onNewIntent, not an ActivityResult -
 * see DropboxBackupProvider) back to whichever screen/ViewModel is waiting for it.
 */
@Singleton
class OAuthRedirectRelay @Inject constructor() {
    private val _redirects = MutableSharedFlow<Uri>(extraBufferCapacity = 1)
    val redirects: SharedFlow<Uri> = _redirects.asSharedFlow()

    fun emit(uri: Uri) {
        _redirects.tryEmit(uri)
    }
}
