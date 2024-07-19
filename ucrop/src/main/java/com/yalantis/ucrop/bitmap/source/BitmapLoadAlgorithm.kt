package com.yalantis.ucrop.bitmap.source

import android.net.Uri
import com.yalantis.ucrop.model.BitmapLoadResult

interface BitmapLoadAlgorithm {
    suspend fun loadBitmapWithExif(from: Uri, reqWidth: Int, reqHeight: Int): BitmapLoadResult
}