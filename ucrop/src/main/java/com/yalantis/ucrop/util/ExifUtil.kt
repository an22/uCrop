package com.yalantis.ucrop.util

import androidx.exifinterface.media.ExifInterface
import com.yalantis.ucrop.model.ExifInfo
import java.io.IOException
import java.io.InputStream

object ExifUtil {

    private const val TAG = "ExifUtil"

    fun exifToDegrees(exifOrientation: Int): Int {
        val rotation = when (exifOrientation) {
            ExifInterface.ORIENTATION_ROTATE_90,
            ExifInterface.ORIENTATION_TRANSPOSE -> 90

            ExifInterface.ORIENTATION_ROTATE_180,
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> 180

            ExifInterface.ORIENTATION_ROTATE_270,
            ExifInterface.ORIENTATION_TRANSVERSE -> 270

            else -> 0
        }
        return rotation
    }

    fun exifToScale(exifOrientation: Int): Int {
        val translation = when (exifOrientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL,
            ExifInterface.ORIENTATION_FLIP_VERTICAL,
            ExifInterface.ORIENTATION_TRANSPOSE,
            ExifInterface.ORIENTATION_TRANSVERSE -> -1

            else -> 1
        }
        return translation
    }

    fun getExifOrientation(eInterface: ExifInterface): Int {
        return try {
            eInterface.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                0
            )
        } catch (e: IOException) {
            Logger.e(TAG, "getExifOrientation: $eInterface", e)
            ExifInterface.ORIENTATION_UNDEFINED
        }
    }

    @Throws(IOException::class)
    fun getExifInterface(stream: InputStream): ExifInterface {
        return stream.use {
            ExifInterface(it)
        }
    }

    fun getExifInfo(eInterface: ExifInterface): ExifInfo {
        val orientation = getExifOrientation(eInterface)
        return ExifInfo(
            exifOrientation = orientation,
            exifScale = exifToScale(orientation),
            exifDegrees = exifToDegrees(orientation)
        )
    }
}