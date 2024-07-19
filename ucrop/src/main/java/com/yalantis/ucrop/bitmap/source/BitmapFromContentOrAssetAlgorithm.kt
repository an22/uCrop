package com.yalantis.ucrop.bitmap.source

import android.content.Context
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import com.yalantis.ucrop.bitmap.BitmapLoadUtils
import com.yalantis.ucrop.bitmap.BitmapLoadUtils.transformBitmap
import com.yalantis.ucrop.model.BitmapLoadResult
import com.yalantis.ucrop.model.FileType
import com.yalantis.ucrop.util.ExifUtil
import java.io.BufferedInputStream
import java.io.IOException

class BitmapFromContentOrAssetAlgorithm(private val context: Context) : BitmapLoadAlgorithm {
    override suspend fun loadBitmapWithExif(from: Uri, reqWidth: Int, reqHeight: Int): BitmapLoadResult {
        val type = FileType.from(from)
        assert(type in listOf(FileType.CONTENT, FileType.FILE)) { "This algorithm can't handle uri of $type" }
        val exifStream = context.contentResolver.openInputStream(from) ?: throw IOException("Can't open stream for uri:$from")
        val exifInterface = exifStream.use { ExifUtil.getExifInterface(exifStream) }
        val exifInfo = ExifUtil.getExifInfo(exifInterface)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, from)) { decoder, _, _ ->
                decoder.setTargetSize(reqWidth, reqHeight)
            }
            BitmapLoadResult(
                transformBitmap(bitmap, exifInfo.transformMatrix),
                exifInterface
            )
        } else {
            val stream = context.contentResolver.openInputStream(from) ?: throw IOException("Can't open stream for uri:$from")
            val bitmap = stream.use {
                BitmapLoadUtils.decodeSampledBitmapFromStream(BufferedInputStream(it), reqWidth, reqHeight)
            }
            BitmapLoadResult(
                transformBitmap(bitmap, exifInfo.transformMatrix),
                exifInterface
            )
        }
    }
}