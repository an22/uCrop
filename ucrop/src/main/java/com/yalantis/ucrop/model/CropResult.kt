package com.yalantis.ucrop.model

import android.net.Uri

class CropResult(
    val resultUri: Uri,
    val offsetX: Int,
    val offsetY: Int,
    val imageWidth: Int,
    val imageHeight: Int
)