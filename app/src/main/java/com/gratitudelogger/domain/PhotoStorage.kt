package com.gratitudelogger.domain

import android.net.Uri
import java.io.File

data class CapturePhotoTarget(val relativePath: String, val uri: Uri)

interface PhotoStorage {
    fun createCaptureTarget(): CapturePhotoTarget
    suspend fun finalizeCapturedPhoto(relativePath: String)
    suspend fun savePickedPhoto(sourceUri: Uri): String
    fun resolveFile(relativePath: String): File
    suspend fun deletePhoto(relativePath: String)
}
