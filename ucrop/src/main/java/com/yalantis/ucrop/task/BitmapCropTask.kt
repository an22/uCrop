package com.yalantis.ucrop.task

import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.net.Uri
import android.os.AsyncTask
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.yalantis.ucrop.callback.BitmapCropCallback
import com.yalantis.ucrop.model.CropParameters
import com.yalantis.ucrop.model.ExifInfo
import com.yalantis.ucrop.model.ImageState
import java.io.File
import java.io.IOException
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Crops part of image that fills the crop bounds.
 *
 *
 * First image is downscaled if max size was set and if resulting image is larger that max size.
 * Then image is rotated accordingly.
 * Finally new Bitmap object is created and saved to file.
 */
class BitmapCropTask(
    private var mViewBitmap: Bitmap?, imageState: ImageState, cropParameters: CropParameters,
    private val mCropCallback: BitmapCropCallback?
) : AsyncTask<Void?, Void?, Throwable?>() {
    private val mCropRect: RectF
    private val mCurrentImageRect: RectF
    private var mCurrentScale: Float
    private val mCurrentAngle: Float
    private val mMaxResultImageSizeX: Int
    private val mMaxResultImageSizeY: Int
    private val mCompressFormat: CompressFormat
    private val mCompressQuality: Int
    private val mImageInputPath: String
    private val mImageOutputPath: String
    private val mExifInfo: ExifInfo
    private var mCroppedImageWidth = 0
    private var mCroppedImageHeight = 0
    private var cropOffsetX = 0
    private var cropOffsetY = 0

    init {
        mCropRect = imageState.cropRect
        mCurrentImageRect = imageState.currentImageRect
        mCurrentScale = imageState.currentScale
        mCurrentAngle = imageState.currentAngle
        mMaxResultImageSizeX = cropParameters.maxResultImageSizeX
        mMaxResultImageSizeY = cropParameters.maxResultImageSizeY
        mCompressFormat = cropParameters.compressFormat
        mCompressQuality = cropParameters.compressQuality
        mImageInputPath = cropParameters.imageInputPath
        mImageOutputPath = cropParameters.imageOutputPath
        mExifInfo = cropParameters.exifInfo
    }

    protected override fun doInBackground(vararg params: Void): Throwable? {
        if (mViewBitmap == null) {
            return NullPointerException("ViewBitmap is null")
        } else if (mViewBitmap!!.isRecycled) {
            return NullPointerException("ViewBitmap is recycled")
        } else if (mCurrentImageRect.isEmpty) {
            return NullPointerException("CurrentImageRect is empty")
        }
        val resizeScale = resize()
        mViewBitmap = try {
            crop(resizeScale)
            null
        } catch (throwable: Throwable) {
            return throwable
        }
        return null
    }

    private fun resize(): Float {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(mImageInputPath, options)
        val swapSides = mExifInfo.exifDegrees == 90 || mExifInfo.exifDegrees == 270
        var scaleX =
            (if (swapSides) options.outHeight else options.outWidth) / mViewBitmap!!.getWidth()
                .toFloat()
        var scaleY =
            (if (swapSides) options.outWidth else options.outHeight) / mViewBitmap!!.getHeight()
                .toFloat()
        var resizeScale = min(scaleX.toDouble(), scaleY.toDouble()).toFloat()
        mCurrentScale /= resizeScale
        resizeScale = 1f
        if (mMaxResultImageSizeX > 0 && mMaxResultImageSizeY > 0) {
            val cropWidth = mCropRect.width() / mCurrentScale
            val cropHeight = mCropRect.height() / mCurrentScale
            if (cropWidth > mMaxResultImageSizeX || cropHeight > mMaxResultImageSizeY) {
                scaleX = mMaxResultImageSizeX / cropWidth
                scaleY = mMaxResultImageSizeY / cropHeight
                resizeScale = min(scaleX.toDouble(), scaleY.toDouble()).toFloat()
                (mCurrentScale /= resizeScale).toFloat()
            }
        }
        return resizeScale
    }

    @Throws(IOException::class)
    private fun crop(resizeScale: Float): Boolean {
        val originalExif = ExifInterface(mImageInputPath)
        cropOffsetX = Math.round((mCropRect.left - mCurrentImageRect.left) / mCurrentScale)
        cropOffsetY = Math.round((mCropRect.top - mCurrentImageRect.top) / mCurrentScale)
        mCroppedImageWidth = Math.round(mCropRect.width() / mCurrentScale)
        mCroppedImageHeight = Math.round(mCropRect.height() / mCurrentScale)
        val shouldCrop = shouldCrop(mCroppedImageWidth, mCroppedImageHeight)
        Log.i(TAG, "Should crop: $shouldCrop")
        return if (shouldCrop) {
            val cropped = cropCImg(
                mImageInputPath, mImageOutputPath,
                cropOffsetX, cropOffsetY, mCroppedImageWidth, mCroppedImageHeight,
                mCurrentAngle, resizeScale, mCompressFormat.ordinal, mCompressQuality,
                mExifInfo.exifDegrees, mExifInfo.exifScale
            )
            if (cropped && mCompressFormat == CompressFormat.JPEG) {
                ImageHeaderParser.copyExif(
                    originalExif,
                    mCroppedImageWidth,
                    mCroppedImageHeight,
                    mImageOutputPath
                )
            }
            cropped
        } else {
            FileUtils.copyFile(mImageInputPath, mImageOutputPath)
            false
        }
    }

    /**
     * Check whether an image should be cropped at all or just file can be copied to the destination path.
     * For each 1000 pixels there is one pixel of error due to matrix calculations etc.
     *
     * @param width  - crop area width
     * @param height - crop area height
     * @return - true if image must be cropped, false - if original image fits requirements
     */
    private fun shouldCrop(width: Int, height: Int): Boolean {
        var pixelError = 1
        (pixelError += Math.round(
            max(
                width.toDouble(),
                height.toDouble()
            ) / 1000f
        )).toInt()
        return mMaxResultImageSizeX > 0 && mMaxResultImageSizeY > 0 || abs((mCropRect.left - mCurrentImageRect.left).toDouble()) > pixelError || abs(
            (mCropRect.top - mCurrentImageRect.top).toDouble()
        ) > pixelError || abs((mCropRect.bottom - mCurrentImageRect.bottom).toDouble()) > pixelError || abs(
            (mCropRect.right - mCurrentImageRect.right).toDouble()
        ) > pixelError || mCurrentAngle != 0f
    }

    override fun onPostExecute(t: Throwable?) {
        if (mCropCallback != null) {
            if (t == null) {
                val uri = Uri.fromFile(File(mImageOutputPath))
                mCropCallback.onBitmapCropped(
                    uri,
                    cropOffsetX,
                    cropOffsetY,
                    mCroppedImageWidth,
                    mCroppedImageHeight
                )
            } else {
                mCropCallback.onCropFailure(t)
            }
        }
    }

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
