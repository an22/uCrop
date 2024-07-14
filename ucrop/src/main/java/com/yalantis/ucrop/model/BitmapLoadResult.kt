package com.yalantis.ucrop.model

import android.graphics.Bitmap
import androidx.exifinterface.media.ExifInterface

class BitmapLoadResult(
    val bitmap: Bitmap,
    val exifInterface: ExifInterface
)