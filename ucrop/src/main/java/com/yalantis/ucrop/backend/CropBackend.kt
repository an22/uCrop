package com.yalantis.ucrop.backend

import android.graphics.Bitmap
import com.yalantis.ucrop.model.CropParameters
import com.yalantis.ucrop.model.CropResult
import com.yalantis.ucrop.model.ImageState

internal interface CropBackend {

    suspend fun crop(
        viewBitmap: Bitmap,
        imageState: ImageState,
        cropParameters: CropParameters
    ): CropResult
}