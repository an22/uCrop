package com.yalantis.ucrop.bitmap

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Point
import android.os.Build
import android.view.Display
import android.view.WindowManager
import com.yalantis.ucrop.util.EglUtils
import com.yalantis.ucrop.util.Logger
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Created by Oleksii Shliama ([...](https://github.com/shliama)).
 */
object BitmapLoadUtils {
    private const val MAX_BITMAP_SIZE = 100 * 1024 * 1024 // 100 MB

    private const val TAG = "BitmapLoadUtils"

    @Suppress("DEPRECATION")
    private val Bitmap.Config.pixelByteCount: Int
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when (this) {
                Bitmap.Config.ALPHA_8 -> 1
                Bitmap.Config.RGB_565,
                Bitmap.Config.ARGB_4444 -> 2

                Bitmap.Config.ARGB_8888,
                Bitmap.Config.RGBA_1010102 -> 4

                Bitmap.Config.RGBA_F16 -> 8
                else -> 0
            }
        } else {
            when (this) {
                Bitmap.Config.ALPHA_8 -> 1
                Bitmap.Config.RGB_565,
                Bitmap.Config.ARGB_4444 -> 2

                else -> 4
            }
        }

    /**
     * This method calculates maximum size of both width and height of bitmap.
     * It is twice the device screen diagonal for default implementation (extra quality to zoom image).
     * Size cannot exceed max texture size.
     *
     * @return - max bitmap size in pixels.
     */
    @JvmStatic
    @Suppress("deprecation")
    fun calculateMaxBitmapSize(context: Context): Int {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        val display: Display
        val width: Int
        val height: Int
        val size = Point()

        if (wm != null) {
            display = wm.defaultDisplay
            display.getSize(size)
        }

        width = size.x
        height = size.y

        // Twice the device screen diagonal as default
        var maxBitmapSize = sqrt(width * width + height * height.toFloat()).toInt()

        // Check for max texture size via Canvas
        val canvas = Canvas()
        val maxCanvasSize = min(canvas.maximumBitmapWidth, canvas.maximumBitmapHeight)
        if (maxCanvasSize > 0) {
            maxBitmapSize = min(maxBitmapSize, maxCanvasSize)
        }

        // Check for max texture size via GL
        val maxTextureSize: Int = EglUtils.maxTextureSize()
        if (maxTextureSize > 0) {
            maxBitmapSize = min(maxBitmapSize, maxTextureSize)
        }

        Logger.d(TAG, "maxBitmapSize: $maxBitmapSize")
        return maxBitmapSize
    }

    /**
     * This method decodes bitmap from uri with these steps
     *
     * 1. Open input stream
     * 2. Downsample image according to size restriction
     * 3. Apply transformations from exif interface
     *
     * @return - max bitmap size in pixels.
     */

    @Throws(FileNotFoundException::class, IOException::class)
    fun decodeSampledBitmapFromStream(
        inputStream: InputStream,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeStream(inputStream, null, options)
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false
        return BitmapFactory.decodeStream(inputStream, null, options)
            ?: throw IOException("Cannot decode stream: $inputStream")
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        // Raw height and width of image
        val height = options.outHeight
        val width = options.outWidth
        //Assume pixel size in bytes without reading bitmap from file
        val worstCasePixelSizeAssumption = 4
        val pixelSize = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            options.outConfig?.pixelByteCount ?: worstCasePixelSizeAssumption
        } else {
            worstCasePixelSizeAssumption
        }
        //Calculate bitmap size in memory after decoding
        var inMemorySize = width * pixelSize * height
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width lower or equal to the requested height and width.
            while ((height / inSampleSize) > reqHeight || (width / inSampleSize) > reqWidth || inMemorySize > MAX_BITMAP_SIZE) {
                inSampleSize *= 2
                inMemorySize /= 2
            }
        }
        return inSampleSize
    }

    fun transformBitmap(bitmap: Bitmap, transformMatrix: Matrix): Bitmap {
        try {
            val converted = Bitmap.createBitmap(
                bitmap,
                0,
                0,
                bitmap.width,
                bitmap.height,
                transformMatrix,
                true
            )
            if (!bitmap.sameAs(converted)) {
                return bitmap
            }
            return converted
        } catch (error: OutOfMemoryError) {
            Logger.e(
                TAG,
                "transformBitmap: Out of memory, exif transformation unavailable, falling back to default bitmap.",
                error
            )
        }
        return bitmap
    }
}