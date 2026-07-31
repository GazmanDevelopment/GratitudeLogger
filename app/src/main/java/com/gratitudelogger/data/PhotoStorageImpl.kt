package com.gratitudelogger.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.gratitudelogger.domain.CapturePhotoTarget
import com.gratitudelogger.domain.PhotoStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import javax.inject.Inject

class PhotoStorageImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : PhotoStorage {

    private val photosDir: File
        get() = File(context.filesDir, "photos").apply { if (!exists()) mkdirs() }

    override fun createCaptureTarget(): CapturePhotoTarget {
        val file = File(photosDir, "${UUID.randomUUID()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return CapturePhotoTarget(relativePath = "photos/${file.name}", uri = uri)
    }

    override suspend fun finalizeCapturedPhoto(relativePath: String) = withContext(Dispatchers.IO) {
        val file = resolveFile(relativePath)
        val bytes = file.readBytes()
        val orientation = ExifInterface(file.absolutePath)
            .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        processAndSave(bytes, orientation, file)
    }

    override suspend fun savePickedPhoto(sourceUri: Uri): String = withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.openInputStream(sourceUri)?.use { it.readBytes() }
            ?: throw IOException("Unable to read picked photo: $sourceUri")
        val orientation = ByteArrayInputStream(bytes).use { stream ->
            ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }
        val file = File(photosDir, "${UUID.randomUUID()}.jpg")
        processAndSave(bytes, orientation, file)
        "photos/${file.name}"
    }

    override fun resolveFile(relativePath: String): File = File(context.filesDir, relativePath)

    override suspend fun deletePhoto(relativePath: String) = withContext(Dispatchers.IO) {
        resolveFile(relativePath).delete()
        Unit
    }

    private fun processAndSave(sourceBytes: ByteArray, exifOrientation: Int, destFile: File) {
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(sourceBytes, 0, sourceBytes.size, boundsOptions)
        var sampleSize = 1
        while (boundsOptions.outWidth / sampleSize > MAX_DIMENSION || boundsOptions.outHeight / sampleSize > MAX_DIMENSION) {
            sampleSize *= 2
        }
        val decoded = BitmapFactory.decodeByteArray(
            sourceBytes, 0, sourceBytes.size,
            BitmapFactory.Options().apply { inSampleSize = sampleSize }
        ) ?: throw IOException("Unable to decode photo")

        val rotated = applyExifRotation(decoded, exifOrientation)
        val finalBitmap = scaleToMax(rotated, MAX_DIMENSION)

        FileOutputStream(destFile).use { out ->
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        }

        if (finalBitmap !== rotated) rotated.recycle()
        if (rotated !== decoded) decoded.recycle()
        finalBitmap.recycle()
    }

    private fun applyExifRotation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun scaleToMax(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val largestSide = maxOf(bitmap.width, bitmap.height)
        if (largestSide <= maxDimension) return bitmap
        val scale = maxDimension.toFloat() / largestSide
        val newWidth = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val newHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    companion object {
        private const val MAX_DIMENSION = 1600
        private const val JPEG_QUALITY = 85
    }
}
