package com.yalantis.ucrop.backend.defaultjvm

import android.graphics.Bitmap
import android.net.Uri
import com.yalantis.ucrop.backend.CropBackend
import com.yalantis.ucrop.model.CropParameters
import com.yalantis.ucrop.model.CropResult
import com.yalantis.ucrop.model.ImageState

internal class DefaultBackend : CropBackend {
    override suspend fun crop(
        viewBitmap: Bitmap,
        imageState: ImageState,
        cropParameters: CropParameters
    ): CropResult {
        assert(!viewBitmap.isRecycled) { "ViewBitmap is recycled" }
        assert(!imageState.currentImageRect.isEmpty) { "CurrentImageRect is empty" }


        return CropResult(Uri.EMPTY, 0, 0, viewBitmap.width, viewBitmap.height)
    }


    private fun cropNonNative(

    ) {

    }
}