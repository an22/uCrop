package com.yalantis.ucrop.model

import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface

/**
 * Created by Oleksii Shliama [https://github.com/shliama] on 6/21/16.
 */
data class ExifInfo(
    val exifOrientation: Int,
    val exifDegrees: Int,
    val exifScale: Int,
    val exif: ExifInterface
) {
    val transformMatrix = Matrix().apply {
        if (exifDegrees != 0) {
            preRotate(exifDegrees.toFloat())
        }
        if (exifScale != 1) {
            postScale(exifScale.toFloat(), 1f)
        }
    }
}
