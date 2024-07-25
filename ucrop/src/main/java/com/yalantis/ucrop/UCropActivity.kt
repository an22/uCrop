package com.yalantis.ucrop

import android.graphics.Bitmap.CompressFormat
import androidx.annotation.IntDef
import androidx.appcompat.app.AppCompatActivity

/**
 * Created by Oleksii Shliama (https://github.com/shliama).
 */
object UCropActivity : AppCompatActivity() {
    const val DEFAULT_COMPRESS_QUALITY: Int = 90
    val DEFAULT_COMPRESS_FORMAT: CompressFormat = CompressFormat.JPEG

    const val NONE: Int = 0
    const val SCALE: Int = 1
    const val ROTATE: Int = 2
    const val ALL: Int = 3

    private const val TAG = "UCropActivity"
    private const val CONTROLS_ANIMATION_DURATION: Long = 50
    private const val TABS_COUNT = 3
    private const val SCALE_WIDGET_SENSITIVITY_COEFFICIENT = 15000
    private const val ROTATE_WIDGET_SENSITIVITY_COEFFICIENT = 42


    @IntDef(NONE, SCALE, ROTATE, ALL)
    @Retention(AnnotationRetention.SOURCE)
    annotation class GestureTypes
}
