package com.yalantis.ucrop.backend

import android.graphics.Bitmap
import com.yalantis.ucrop.BuildConfig
import com.yalantis.ucrop.backend.defaultjvm.DefaultBackend
import com.yalantis.ucrop.backend.nativecpp.NativeBackend
import com.yalantis.ucrop.model.CropParameters
import com.yalantis.ucrop.model.CropResult
import com.yalantis.ucrop.model.ImageState

class UCropBackendImpl : CropBackend {

    private val implementation = createBackend()

    override suspend fun crop(
        viewBitmap: Bitmap,
        imageState: ImageState,
        cropParameters: CropParameters
    ): CropResult {
        assert(!viewBitmap.isRecycled) { "ViewBitmap is recycled" }
        assert(!imageState.currentImageRect.isEmpty) { "CurrentImageRect is empty" }

        return implementation.crop(viewBitmap, imageState, cropParameters)
    }

    private fun createBackend(): CropBackend {
        return when (BuildConfig.TYPE) {
            UCropBackendType.NATIVE.label -> NativeBackend()
            else -> DefaultBackend()
        }
    }
}