package com.yalantis.ucrop.backend.nativecpp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import com.yalantis.ucrop.backend.UCropBackend
import com.yalantis.ucrop.model.CropParameters
import com.yalantis.ucrop.model.CropResult
import com.yalantis.ucrop.model.ImageState
import com.yalantis.ucrop.model.ScaleInfo
import com.yalantis.ucrop.task.BitmapCropTask
import com.yalantis.ucrop.util.FileUtils
import com.yalantis.ucrop.util.ImageHeaderParser
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.io.IOException
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import android.os.FileUtils as AndroidFileUtils

internal class NativeBackend(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : UCropBackend {

    override suspend fun crop(
        viewBitmap: Bitmap,
        imageState: ImageState,
        cropParameters: CropParameters
    ): Result<CropResult> = with(dispatcher) {
        assert(!viewBitmap.isRecycled) { "ViewBitmap is recycled" }
        assert(!imageState.currentImageRect.isEmpty) { "CurrentImageRect is empty" }
        runCatching {
            val scaleInfo = resize(viewBitmap, imageState, cropParameters)
            cropNative(imageState, cropParameters, scaleInfo)
        }

    }

    @Throws(IOException::class)
    private fun resize(
        viewBitmap: Bitmap,
        imageState: ImageState,
        cropParameters: CropParameters
    ): ScaleInfo {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(cropParameters.imageInputPath, options)
        val shouldSwapSides =
            cropParameters.exifInfo.exifDegrees == 90 || cropParameters.exifInfo.exifDegrees == 270
        val xSource = (if (shouldSwapSides) options.outHeight else options.outWidth)
        val ySource = (if (shouldSwapSides) options.outWidth else options.outHeight)
        var scaleX = xSource / viewBitmap.width.toFloat()
        var scaleY = ySource / viewBitmap.height.toFloat()
        var resizeScale = min(scaleX, scaleY)
        var scale = imageState.currentScale / resizeScale
        if (cropParameters.maxResultImageSizeX > 0 && cropParameters.maxResultImageSizeY > 0) {
            val cropWidth = imageState.cropRect.width() / scale
            val cropHeight = imageState.cropRect.height() / scale
            if (cropWidth > cropParameters.maxResultImageSizeX || cropHeight > cropParameters.maxResultImageSizeY) {
                scaleX = cropParameters.maxResultImageSizeX / cropWidth
                scaleY = cropParameters.maxResultImageSizeY / cropHeight
                resizeScale = min(scaleX, scaleY)
                scale /= resizeScale
            }
        }
        return ScaleInfo(scale, resizeScale)
    }

    @Throws(IOException::class)
    private fun cropNative(
        imageState: ImageState,
        cropParameters: CropParameters,
        scaleInfo: ScaleInfo
    ): CropResult {
        val originalExif = ExifInterface(cropParameters.imageInputPath)
        val cropOffsetX =
            round((imageState.cropRect.left - imageState.cropRect.left) / scaleInfo.currentScale)
        val cropOffsetY =
            round((imageState.cropRect.top - imageState.cropRect.top) / scaleInfo.currentScale)
        val croppedImageWidth = round(imageState.cropRect.width() / scaleInfo.currentScale).toInt()
        val croppedImageHeight =
            round(imageState.cropRect.height() / scaleInfo.currentScale).toInt()
        val shouldCrop = shouldCrop(
            croppedImageWidth,
            croppedImageHeight,
            cropParameters.maxResultImageSizeX,
            cropParameters.maxResultImageSizeY,
            imageState.cropRect,
            imageState.currentImageRect,
            imageState.currentAngle
        )
        if (shouldCrop) {
            val cropped = BitmapCropTask.cropCImg(
                cropParameters.imageInputPath,
                cropParameters.imageOutputPath,
                cropOffsetX.toInt(),
                cropOffsetY.toInt(),
                croppedImageWidth,
                croppedImageHeight,
                imageState.currentAngle,
                scaleInfo.resizeScale,
                cropParameters.compressFormat.ordinal,
                cropParameters.compressQuality,
                cropParameters.exifInfo.exifDegrees,
                cropParameters.exifInfo.exifTranslation
            )
            if (cropped && cropParameters.compressFormat == Bitmap.CompressFormat.JPEG) {
                ImageHeaderParser.copyExif(
                    originalExif,
                    croppedImageWidth,
                    croppedImageHeight,
                    cropParameters.imageOutputPath
                )
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                AndroidFileUtils.copy(
                    File(cropParameters.imageInputPath).inputStream(),
                    File(cropParameters.imageOutputPath).outputStream()
                )
            } else {
                FileUtils.copyFile(cropParameters.imageInputPath, cropParameters.imageOutputPath)
            }
        }
        return CropResult(
            Uri.fromFile(File(cropParameters.imageOutputPath)),
            cropOffsetX.toInt(),
            cropOffsetY.toInt(),
            croppedImageWidth,
            croppedImageHeight
        )
    }

    private fun shouldCrop(
        width: Int,
        height: Int,
        maxImageSizeX: Int,
        maxImageSizeY: Int,
        cropRect: RectF,
        currentImageRect: RectF,
        rotationAngle: Float
    ): Boolean {
        val pixelError = 1 + round(max(width.toDouble(), height.toDouble()) / 1000f).toInt()
        val isNonFlat = maxImageSizeX > 0 && maxImageSizeY > 0
        val leftBorderChanged = abs(cropRect.left - currentImageRect.left) > pixelError
        val topBorderChanged = abs(cropRect.top - currentImageRect.top) > pixelError
        val bottomBorderChanged = abs(cropRect.bottom - currentImageRect.bottom) > pixelError
        val rightBorderChanged = abs(cropRect.right - currentImageRect.right) > pixelError
        val angleChanged = rotationAngle != 0f
        return isNonFlat or leftBorderChanged or topBorderChanged or bottomBorderChanged or rightBorderChanged or angleChanged
    }
}