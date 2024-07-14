package com.yalantis.ucrop.bitmap.algorithm

import android.net.Uri
import com.yalantis.ucrop.model.BitmapLoadResult

interface BitmapLoadAlgorithm {
    suspend fun loadBitmapWithExif(from: Uri, reqWidth: Int, reqHeight: Int): BitmapLoadResult
}