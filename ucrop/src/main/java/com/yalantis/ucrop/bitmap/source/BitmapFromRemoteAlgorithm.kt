package com.yalantis.ucrop.bitmap.source

import android.content.Context
import android.net.Uri
import com.yalantis.ucrop.UCrop.Companion.UCROP_CACHE_DIR_NAME
import com.yalantis.ucrop.UCropHttpClientStore
import com.yalantis.ucrop.bitmap.BitmapLoadUtils
import com.yalantis.ucrop.bitmap.BitmapLoadUtils.transformBitmap
import com.yalantis.ucrop.model.BitmapLoadResult
import com.yalantis.ucrop.model.FileType
import com.yalantis.ucrop.util.ExifUtil
import okhttp3.Request
import java.io.BufferedInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream

class BitmapFromRemoteAlgorithm(private val context: Context) : BitmapLoadAlgorithm {
    override suspend fun loadBitmapWithExif(from: Uri, reqWidth: Int, reqHeight: Int): BitmapLoadResult {
        val type = FileType.from(from)
        assert(type == FileType.REMOTE) { "This algorithm can't handle uri of $type" }

        val response = UCropHttpClientStore.INSTANCE.client
            .newCall(
                Request.Builder()
                    .url(from.toString())
                    .build()
            ).execute()

        val stream = response.body?.byteStream() ?: throw IOException("Failed to download bitmap from $from")
        val cacheDir = File(context.cacheDir, UCROP_CACHE_DIR_NAME)
        val cacheFile = File(cacheDir, response.request.url.pathSegments.last())
        try {
            createAndFillCacheFile(cacheDir, cacheFile, stream)
            val exifInterface = cacheFile.inputStream().use { ExifUtil.getExifInterface(it) }
            val exifInfo = ExifUtil.getExifInfo(exifInterface)
            val bitmap = cacheFile.inputStream().use {
                BitmapLoadUtils.decodeSampledBitmapFromStream(BufferedInputStream(it), reqWidth, reqHeight)
            }
            return BitmapLoadResult(
                transformBitmap(bitmap, exifInfo.transformMatrix),
                exifInterface
            )
        } finally {
            if (cacheFile.exists()) {
                cacheFile.delete()
            }
        }
    }

    private fun createAndFillCacheFile(
        cacheDir: File,
        cacheFile: File,
        stream: InputStream
    ) {
        cacheDir.mkdir()
        if(cacheFile.exists()) cacheFile.delete()
        cacheFile.createNewFile()
        val oStream = cacheFile.outputStream()
        stream.use {
            oStream.use {
                stream.copyTo(oStream)
            }
        }
    }
}