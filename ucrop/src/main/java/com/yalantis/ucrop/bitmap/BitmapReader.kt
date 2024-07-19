package com.yalantis.ucrop.bitmap

import android.content.Context
import android.net.Uri
import com.yalantis.ucrop.bitmap.source.BitmapFromContentOrAssetAlgorithm
import com.yalantis.ucrop.bitmap.source.BitmapFromFileAlgorithm
import com.yalantis.ucrop.bitmap.source.BitmapFromRemoteAlgorithm
import com.yalantis.ucrop.bitmap.source.BitmapLoadAlgorithm
import com.yalantis.ucrop.model.BitmapLoadResult
import com.yalantis.ucrop.model.FileType

class BitmapReader {

    suspend fun readBitmap(context: Context, from: Uri, reqWidth: Int, reqHeight: Int): BitmapLoadResult {
        val algorithm = createAlgorithm(context, FileType.from(from), from.scheme.orEmpty())
        return algorithm.loadBitmapWithExif(from, reqWidth, reqHeight)
    }

    private fun createAlgorithm(context: Context, fileType: FileType, uriScheme: String): BitmapLoadAlgorithm {
        return when (fileType) {
            FileType.REMOTE -> BitmapFromRemoteAlgorithm(context)
            FileType.CONTENT -> BitmapFromContentOrAssetAlgorithm(context)
            FileType.FILE -> BitmapFromFileAlgorithm()
            FileType.UNKNOWN -> throw IllegalArgumentException("Can't read bitmap from unsupported uri scheme: $uriScheme")
        }
    }
}