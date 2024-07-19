package com.yalantis.ucrop.task

import java.io.IOException

/**
 * Crops part of image that fills the crop bounds.
 *
 *
 * First image is downscaled if max size was set and if resulting image is larger that max size.
 * Then image is rotated accordingly.
 * Finally new Bitmap object is created and saved to file.
 */
class BitmapCropTask {
    companion object {
        private const val TAG = "BitmapCropTask"

        init {
            System.loadLibrary("ucrop")
        }

        @Throws(IOException::class, OutOfMemoryError::class)
        external fun cropCImg(
            inputPath: String?, outputPath: String?,
            left: Int, top: Int, width: Int, height: Int,
            angle: Float, resizeScale: Float,
            format: Int, quality: Int,
            exifDegrees: Int, exifTranslation: Int
        ): Boolean
    }
}
