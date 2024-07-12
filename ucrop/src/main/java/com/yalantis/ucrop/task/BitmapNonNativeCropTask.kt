package com.yalantis.ucrop.task

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.Matrix
import android.graphics.RectF
import android.net.Uri
import android.os.AsyncTask
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.yalantis.ucrop.callback.BitmapCropCallback
import com.yalantis.ucrop.model.CropParameters
import com.yalantis.ucrop.model.ExifInfo
import com.yalantis.ucrop.model.ImageState
import com.yalantis.ucrop.util.BitmapLoadUtils
import com.yalantis.ucrop.util.FileUtils
import com.yalantis.ucrop.util.ImageHeaderParser
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.lang.ref.WeakReference
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
class BitmapNonNativeCropTask(
    context: Context,
    private var mViewBitmap: Bitmap?,
    imageState: ImageState,
    cropParameters: CropParameters,
    private val mCropCallback: BitmapCropCallback?
) : AsyncTask<Void?, Void?, Throwable?>() {
    private val mContext: WeakReference<Context>
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
    private val mImageInputUri: Uri?
    private val mImageOutputUri: Uri?
    private val mExifInfo: ExifInfo
    private var mCroppedImageWidth = 0
    private var mCroppedImageHeight = 0
    private var cropOffsetX = 0
    private var cropOffsetY = 0

    init {
        mContext = WeakReference(context)
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
        mImageInputUri = cropParameters.contentImageInputUri
        mImageOutputUri = cropParameters.contentImageOutputUri
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
        if (mImageOutputUri == null) {
            return NullPointerException("ImageOutputUri is null")
        }
        mViewBitmap = try {
            crop()
            null
        } catch (throwable: Throwable) {
            return throwable
        }
        return null
    }

    @Throws(IOException::class)
    private fun crop(): Boolean {
        val context = mContext.get() ?: return false

        // Downsize if needed
        if (mMaxResultImageSizeX > 0 && mMaxResultImageSizeY > 0) {
            val cropWidth = mCropRect.width() / mCurrentScale
            val cropHeight = mCropRect.height() / mCurrentScale
            if (cropWidth > mMaxResultImageSizeX || cropHeight > mMaxResultImageSizeY) {
                val scaleX = mMaxResultImageSizeX / cropWidth
                val scaleY = mMaxResultImageSizeY / cropHeight
                val resizeScale = min(scaleX.toDouble(), scaleY.toDouble())
                    .toFloat()
                val resizedBitmap = Bitmap.createScaledBitmap(
                    mViewBitmap!!,
                    Math.round(mViewBitmap!!.getWidth() * resizeScale),
                    Math.round(mViewBitmap!!.getHeight() * resizeScale), false
                )
                if (mViewBitmap != resizedBitmap) {
                    mViewBitmap!!.recycle()
                }
                mViewBitmap = resizedBitmap
                mCurrentScale /= resizeScale
            }
        }

        // Rotate if needed
        if (mCurrentAngle != 0f) {
            val tempMatrix = Matrix()
            tempMatrix.setRotate(
                mCurrentAngle,
                (mViewBitmap!!.getWidth() / 2).toFloat(),
                (mViewBitmap!!.getHeight() / 2).toFloat()
            )
            val rotatedBitmap = Bitmap.createBitmap(
                mViewBitmap!!, 0, 0, mViewBitmap!!.getWidth(), mViewBitmap!!.getHeight(),
                tempMatrix, true
            )
            if (mViewBitmap != rotatedBitmap) {
                mViewBitmap!!.recycle()
            }
            mViewBitmap = rotatedBitmap
        }
        cropOffsetX = Math.round((mCropRect.left - mCurrentImageRect.left) / mCurrentScale)
        cropOffsetY = Math.round((mCropRect.top - mCurrentImageRect.top) / mCurrentScale)
        mCroppedImageWidth = Math.round(mCropRect.width() / mCurrentScale)
        mCroppedImageHeight = Math.round(mCropRect.height() / mCurrentScale)
        val shouldCrop = shouldCrop(mCroppedImageWidth, mCroppedImageHeight)
        Log.i(TAG, "Should crop: $shouldCrop")
        return if (shouldCrop) {
            saveImage(
                Bitmap.createBitmap(
                    mViewBitmap!!,
                    cropOffsetX,
                    cropOffsetY,
                    mCroppedImageWidth,
                    mCroppedImageHeight
                )
            )
            if (mCompressFormat == CompressFormat.JPEG) {
                copyExifForOutputFile(context)
            }
            true
        } else {
            FileUtils.copyFile(context, mImageInputUri!!, mImageOutputUri!!)
            false
        }
    }

    @Throws(IOException::class)
    private fun copyExifForOutputFile(context: Context) {
        val hasImageInputUriContentSchema = BitmapLoadUtils.hasContentScheme(mImageInputUri)
        val hasImageOutputUriContentSchema = BitmapLoadUtils.hasContentScheme(mImageOutputUri)
        /*
         * ImageHeaderParser.copyExif with output uri as a parameter
         * uses ExifInterface constructor with FileDescriptor param for overriding output file exif info,
         * which doesn't support ExitInterface.saveAttributes call for SDK lower than 21.
         *
         * See documentation for ImageHeaderParser.copyExif and ExifInterface.saveAttributes implementation.
         */if (hasImageInputUriContentSchema && hasImageOutputUriContentSchema) {
            ImageHeaderParser.copyExif(
                context,
                mCroppedImageWidth,
                mCroppedImageHeight,
                mImageInputUri,
                mImageOutputUri
            )
        } else if (hasImageInputUriContentSchema) {
            ImageHeaderParser.copyExif(
                context,
                mCroppedImageWidth,
                mCroppedImageHeight,
                mImageInputUri,
                mImageOutputPath
            )
        } else if (hasImageOutputUriContentSchema) {
            val originalExif = ExifInterface(mImageInputPath)
            ImageHeaderParser.copyExif(
                context,
                originalExif,
                mCroppedImageWidth,
                mCroppedImageHeight,
                mImageOutputUri
            )
        } else {
            val originalExif = ExifInterface(mImageInputPath)
            ImageHeaderParser.copyExif(
                originalExif,
                mCroppedImageWidth,
                mCroppedImageHeight,
                mImageOutputPath
            )
        }
    }

    private fun saveImage(croppedBitmap: Bitmap) {
        val context = mContext.get() ?: return
        var outputStream: OutputStream? = null
        var outStream: ByteArrayOutputStream? = null
        try {
            outputStream = context.contentResolver.openOutputStream(mImageOutputUri!!)
            outStream = ByteArrayOutputStream()
            croppedBitmap.compress(mCompressFormat, mCompressQuality, outStream)
            outputStream!!.write(outStream.toByteArray())
            croppedBitmap.recycle()
        } catch (exc: IOException) {
            Log.e(TAG, exc.localizedMessage)
        } finally {
            BitmapLoadUtils.close(outputStream)
            BitmapLoadUtils.close(outStream)
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
                val uri: Uri?
                uri = if (BitmapLoadUtils.hasContentScheme(mImageOutputUri)) {
                    mImageOutputUri
                } else {
                    Uri.fromFile(File(mImageOutputPath))
                }
                mCropCallback.onBitmapCropped(
                    uri!!,
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
        private const val CONTENT_SCHEME = "content"
    }
}
