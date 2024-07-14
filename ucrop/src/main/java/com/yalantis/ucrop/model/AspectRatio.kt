package com.yalantis.ucrop.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Created by Oleksii Shliama [https://github.com/shliama] on 6/24/16.
 */
@Parcelize
open class AspectRatio(
    val aspectRatioTitle: String?,
    val aspectRatioX: Float,
    val aspectRatioY: Float
) : Parcelable
