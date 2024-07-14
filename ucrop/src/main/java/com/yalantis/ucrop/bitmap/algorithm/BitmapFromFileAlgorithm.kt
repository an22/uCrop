package com.yalantis.ucrop.bitmap.algorithm

import android.net.Uri
import com.yalantis.ucrop.bitmap.BitmapLoadUtils
import com.yalantis.ucrop.bitmap.BitmapLoadUtils.transformBitmap
import com.yalantis.ucrop.model.BitmapLoadResult
import com.yalantis.ucrop.model.FileType
import com.yalantis.ucrop.util.ExifUtil
import java.io.File

class BitmapFromFileAlgorithm : BitmapLoadAlgorithm {
    override suspend fun loadBitmapWithExif(from: Uri, reqWidth: Int, reqHeight: Int): BitmapLoadResult {
        val type = FileType.from(from)
        assert(type != FileType.FILE) { "This algorithm can't handle uri of $type" }
        val file = File(from.path.orEmpty())
        val exifStream = file.inputStream()
        val exifInterface = exifStream.use { ExifUtil.getExifInterface(exifStream) }
        val exifInfo = ExifUtil.getExifInfo(exifInterface)
        val bitmap = file.inputStream().use {
            BitmapLoadUtils.decodeSampledBitmapFromStream(it, reqWidth, reqHeight)
        }
        return BitmapLoadResult(
            transformBitmap(bitmap, exifInfo.transformMatrix),
            exifInterface
        )
    }
}