package com.yalantis.ucrop.task

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.AsyncTask
import android.util.Log
import com.yalantis.ucrop.UCropHttpClientStore
import com.yalantis.ucrop.callback.BitmapLoadCallback
import com.yalantis.ucrop.model.ExifInfo
import com.yalantis.ucrop.task.BitmapLoadTask.BitmapWorkerResult
import com.yalantis.ucrop.util.BitmapLoadUtils
import okhttp3.Request
import okhttp3.Request.Builder.build
import okhttp3.Request.Builder.url
import okhttp3.Response
import okio.BufferedSource
import okio.Sink
import okio.sink
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.ref.WeakReference

/**
 * Creates and returns a Bitmap for a given Uri(String url).
 * inSampleSize is calculated based on requiredWidth property. However can be adjusted if OOM occurs.
 * If any EXIF config is found - bitmap is transformed properly.
 */
class BitmapLoadTask(
    context: Context,
    inputUri: Uri,
    outputUri: Uri?,
    requiredWidth: Int,
    requiredHeight: Int,
    loadCallback: BitmapLoadCallback
) : AsyncTask<Void?, Void?, BitmapWorkerResult>() {
    private val mContext: WeakReference<Context>
    private var mInputUri: Uri?
    private val mOutputUri: Uri?
    private val mRequiredWidth: Int
    private val mRequiredHeight: Int
    private val mBitmapLoadCallback: BitmapLoadCallback

    class BitmapWorkerResult {
        var mBitmapResult: Bitmap? = null
        var mExifInfo: ExifInfo? = null
        var mBitmapWorkerException: Exception? = null

        constructor(bitmapResult: Bitmap, exifInfo: ExifInfo) {
            mBitmapResult = bitmapResult
            mExifInfo = exifInfo
        }

        constructor(bitmapWorkerException: Exception) {
            mBitmapWorkerException = bitmapWorkerException
        }
    }

    init {
        mContext = WeakReference(context)
        mInputUri = inputUri
        mOutputUri = outputUri
        mRequiredWidth = requiredWidth
        mRequiredHeight = requiredHeight
        mBitmapLoadCallback = loadCallback
    }

    protected override fun doInBackground(vararg params: Void): BitmapWorkerResult {
        val context = mContext.get()
            ?: return BitmapWorkerResult(NullPointerException("context is null"))
        if (mInputUri == null) {
            return BitmapWorkerResult(NullPointerException("Input Uri cannot be null"))
        }
        try {
            processInputUri()
        } catch (e: NullPointerException) {
            return BitmapWorkerResult(e)
        } catch (e: IOException) {
            return BitmapWorkerResult(e)
        }
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        options.inSampleSize =
            BitmapLoadUtils.calculateInSampleSize(options, mRequiredWidth, mRequiredHeight)
        options.inJustDecodeBounds = false
        var decodeSampledBitmap: Bitmap? = null
        var decodeAttemptSuccess = false
        while (!decodeAttemptSuccess) {
            try {
                val stream = context.contentResolver.openInputStream(
                    mInputUri!!
                )
                try {
                    decodeSampledBitmap = BitmapFactory.decodeStream(stream, null, options)
                    if (options.outWidth == -1 || options.outHeight == -1) {
                        return BitmapWorkerResult(IllegalArgumentException("Bounds for bitmap could not be retrieved from the Uri: [$mInputUri]"))
                    }
                } finally {
                    BitmapLoadUtils.close(stream)
                }
                if (checkSize(decodeSampledBitmap, options)) continue
                decodeAttemptSuccess = true
            } catch (error: OutOfMemoryError) {
                Log.e(TAG, "doInBackground: BitmapFactory.decodeFileDescriptor: ", error)
                options.inSampleSize *= 2
            } catch (e: IOException) {
                Log.e(TAG, "doInBackground: ImageDecoder.createSource: ", e)
                return BitmapWorkerResult(
                    IllegalArgumentException(
                        "Bitmap could not be decoded from the Uri: [$mInputUri]",
                        e
                    )
                )
            }
        }
        if (decodeSampledBitmap == null) {
            return BitmapWorkerResult(IllegalArgumentException("Bitmap could not be decoded from the Uri: [$mInputUri]"))
        }
        val exifOrientation = BitmapLoadUtils.getExifOrientation(context, mInputUri!!)
        val exifDegrees = BitmapLoadUtils.exifToDegrees(exifOrientation)
        val exifTranslation = BitmapLoadUtils.exifToTranslation(exifOrientation)
        val exifInfo = ExifInfo(exifOrientation, exifDegrees, exifTranslation)
        val matrix = Matrix()
        if (exifDegrees != 0) {
            matrix.preRotate(exifDegrees.toFloat())
        }
        if (exifTranslation != 1) {
            matrix.postScale(exifTranslation.toFloat(), 1f)
        }
        return if (!matrix.isIdentity) {
            BitmapWorkerResult(
                BitmapLoadUtils.transformBitmap(decodeSampledBitmap, matrix),
                exifInfo
            )
        } else BitmapWorkerResult(decodeSampledBitmap, exifInfo)
    }

    @Throws(NullPointerException::class, IOException::class)
    private fun processInputUri() {
        Log.d(TAG, "Uri scheme: " + mInputUri!!.scheme)
        if (isDownloadUri(mInputUri)) {
            try {
                downloadFile(mInputUri!!, mOutputUri)
            } catch (e: NullPointerException) {
                Log.e(TAG, "Downloading failed", e)
                throw e
            } catch (e: IOException) {
                Log.e(TAG, "Downloading failed", e)
                throw e
            }
        } else if (isContentUri(mInputUri)) {
            try {
                copyFile(mInputUri!!, mOutputUri)
            } catch (e: NullPointerException) {
                Log.e(TAG, "Copying failed", e)
                throw e
            } catch (e: IOException) {
                Log.e(TAG, "Copying failed", e)
                throw e
            }
        } else if (!isFileUri(mInputUri)) {
            val inputUriScheme = mInputUri!!.scheme
            Log.e(TAG, "Invalid Uri scheme $inputUriScheme")
            throw IllegalArgumentException("Invalid Uri scheme$inputUriScheme")
        }
    }

    @Throws(NullPointerException::class, IOException::class)
    private fun copyFile(inputUri: Uri, outputUri: Uri?) {
        Log.d(TAG, "copyFile")
        if (outputUri == null) {
            throw NullPointerException("Output Uri is null - cannot copy image")
        }
        val context = mContext.get()
        var inputStream: InputStream? = null
        var outputStream: OutputStream? = null
        try {
            inputStream = context!!.contentResolver.openInputStream(inputUri)
            if (inputStream == null) {
                throw NullPointerException("InputStream for given input Uri is null")
            }
            outputStream = if (isContentUri(outputUri)) {
                context.contentResolver.openOutputStream(outputUri)
            } else {
                FileOutputStream(File(outputUri.path))
            }
            val buffer = ByteArray(1024)
            var length: Int
            while (inputStream.read(buffer).also { length = it } > 0) {
                outputStream!!.write(buffer, 0, length)
            }
        } finally {
            BitmapLoadUtils.close(outputStream)
            BitmapLoadUtils.close(inputStream)

            // swap uris, because input image was copied to the output destination
            // (cropped image will override it later)
            mInputUri = mOutputUri
        }
    }

    @Throws(NullPointerException::class, IOException::class)
    private fun downloadFile(inputUri: Uri, outputUri: Uri?) {
        Log.d(TAG, "downloadFile")
        if (outputUri == null) {
            throw NullPointerException("Output Uri is null - cannot download image")
        }
        val context = mContext.get() ?: throw NullPointerException("Context is null")
        val client = UCropHttpClientStore.INSTANCE.getClient()
        var source: BufferedSource? = null
        var sink: Sink? = null
        var response: Response? = null
        try {
            val request: Request = Builder()
                .url(inputUri.toString())
                .build()
            response = client.newCall(request).execute()
            source = response.body()!!.source()
            val outputStream: OutputStream?
            outputStream = if (isContentUri(mOutputUri)) {
                context.contentResolver.openOutputStream(outputUri)
            } else {
                FileOutputStream(File(outputUri.path))
            }
            if (outputStream != null) {
                sink = outputStream.sink()
                source.readAll(sink)
            } else {
                throw NullPointerException("OutputStream for given output Uri is null")
            }
        } finally {
            BitmapLoadUtils.close(source)
            BitmapLoadUtils.close(sink)
            if (response != null) {
                BitmapLoadUtils.close(response.body())
            }
            client.dispatcher.cancelAll()

            // swap uris, because input image was downloaded to the output destination
            // (cropped image will override it later)
            mInputUri = mOutputUri
        }
    }

    override fun onPostExecute(result: BitmapWorkerResult) {
        if (result.mBitmapWorkerException == null) {
            mBitmapLoadCallback.onBitmapLoaded(
                result.mBitmapResult!!,
                result.mExifInfo!!,
                mInputUri!!,
                mOutputUri
            )
        } else {
            mBitmapLoadCallback.onFailure(result.mBitmapWorkerException!!)
        }
    }

    private fun checkSize(bitmap: Bitmap?, options: BitmapFactory.Options): Boolean {
        val bitmapSize = bitmap?.getByteCount() ?: 0
        if (bitmapSize > MAX_BITMAP_SIZE) {
            options.inSampleSize *= 2
            return true
        }
        return false
    }

    private fun isDownloadUri(uri: Uri?): Boolean {
        val schema = uri!!.scheme
        return schema == "http" || schema == "https"
    }

    private fun isContentUri(uri: Uri?): Boolean {
        val schema = uri!!.scheme
        return schema == "content"
    }

    private fun isFileUri(uri: Uri?): Boolean {
        val schema = uri!!.scheme
        return schema == "file"
    }

    companion object {
        private const val TAG = "BitmapWorkerTask"
        private const val MAX_BITMAP_SIZE = 100 * 1024 * 1024 // 100 MB
    }
}
