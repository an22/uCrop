package com.yalantis.ucrop.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Rect
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.graphics.values
import com.yalantis.ucrop.gesture.RotationGestureDetector
import com.yalantis.ucrop.util.Logger
import com.yalantis.ucrop.view.adapter.KotlinOnGestureListener
import com.yalantis.ucrop.view.adapter.KotlinOnRotateListener
import com.yalantis.ucrop.view.adapter.KotlinOnScaleListener
import com.yalantis.ucrop.view.drawable.CropOverlayDrawable

/**
 * Created by Oleksii Shliama (https://github.com/shliama).
 */
open class GestureCropImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : AppCompatImageView(context, attrs, defStyle) {

    private val mScaleDetector = ScaleGestureDetector(context, KotlinOnScaleListener(onScale = {
        drawMatrix.postScale(it.scaleFactor, it.scaleFactor, touchCenterX, touchCenterY)
        imageMatrix = drawMatrix
        true
    }))
    private val mRotateDetector = RotationGestureDetector(KotlinOnRotateListener(onRotation = {
        drawMatrix.postRotate(it.angle, touchCenterX, touchCenterY)
        imageMatrix = drawMatrix
        true
    }))
    private val mGestureDetector = GestureDetector(context, KotlinOnGestureListener(
        onScroll = { _, _, distanceX: Float, distanceY: Float ->
            drawMatrix.postTranslate(-distanceX, -distanceY)
            imageMatrix = drawMatrix
        },
        onDoubleTap = {
            val scaleFactor = DOUBLE_TAP_SCALE_FACTOR
            drawMatrix.postTranslate((width / 2f) - it.x, (height / 2f) - it.y)
            drawMatrix.postScale(scaleFactor, scaleFactor, width / 2f, height / 2f)
            imageMatrix = drawMatrix
//            zoomImageToPosition(
//                doubleTapTargetScale,
//                it.x,
//                it.y,
//                DOUBLE_TAP_ZOOM_DURATION.toLong()
//            )
        }
    ), null, true)

    var isRotateEnabled: Boolean = true
    var isScaleEnabled: Boolean = true
    var isGestureEnabled: Boolean = true
    var doubleTapScaleSteps: Int = 5

    private var touchCenterX = 0f
    private var touchCenterY = 0f

    private val drawMatrix = Matrix()
    private var bitmapSourceBounds = FloatArray(8)
    private val bitmapActualBounds = FloatArray(8)
    private val overlayDrawable = CropOverlayDrawable()

    override fun setImageBitmap(bm: Bitmap?) {
        scaleType = ScaleType.FIT_CENTER
        super.setImageBitmap(bm)
        scaleType = ScaleType.MATRIX
        drawMatrix.setValues(imageMatrix.values())
        bitmapSourceBounds[0] = 0f
        bitmapSourceBounds[1] = 0f

        bitmapSourceBounds[2] = drawable.intrinsicWidth.toFloat()
        bitmapSourceBounds[3] = 0f

        bitmapSourceBounds[4] = drawable.intrinsicWidth.toFloat()
        bitmapSourceBounds[5] = drawable.intrinsicHeight.toFloat()

        bitmapSourceBounds[6] = 0f
        bitmapSourceBounds[7] = drawable.intrinsicHeight.toFloat()

        bitmapSourceBounds.copyInto(bitmapActualBounds)

        drawMatrix.mapPoints(bitmapActualBounds)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        overlayDrawable.setBounds(0, 0, w, w)
        overlayDrawable.setClipAreaBounds(Rect(50, 50, w - 50, h - 50))
    }

    override fun setImageMatrix(matrix: Matrix?) {
        super.setImageMatrix(matrix)
        bitmapSourceBounds.copyInto(bitmapActualBounds)
        matrix?.mapPoints(bitmapActualBounds)
    }

    /**
     * If it's ACTION_DOWN event - user touches the screen and all current animation must be canceled.
     * If it's ACTION_UP event - user removed all fingers from the screen and current image position must be corrected.
     * If there are more than 2 fingers - update focal point coordinates.
     * Pass the event to the gesture detectors if those are enabled.
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if ((event.action and MotionEvent.ACTION_MASK) == MotionEvent.ACTION_DOWN) {
//            cancelAllAnimations()
        }

        if (event.pointerCount > 1) {
            touchCenterX = (event.getX(0) + event.getX(1)) / 2
            touchCenterY = (event.getY(0) + event.getY(1)) / 2
        }

        if (isGestureEnabled) {
            mGestureDetector.onTouchEvent(event)
        }

        if (isScaleEnabled) {
            mScaleDetector.onTouchEvent(event)
        }

        if (isRotateEnabled) {
            mRotateDetector.onTouchEvent(event)
        }

        if ((event.action and MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
            wrapCropBounds()
        }
        return true
    }

    private fun wrapCropBounds() {
        val cropBounds = overlayDrawable.cropAreaRect

        var scaleFactor = 1f
        var shouldScale = false
        for(i in bitmapActualBounds.indices step 2) {
            val x = bitmapActualBounds[i]
            val y = bitmapActualBounds[i + 1]

            if(!cropBounds.contains(x.toInt(), y.toInt())) {
                shouldScale = true
                break
            }
        }
        if(shouldScale) {

        }
    }

    override fun getScaleType(): ScaleType {
        return ScaleType.MATRIX
    }

    override fun setScaleType(scaleType: ScaleType?) {
        if (scaleType != ScaleType.MATRIX) {
            Logger.w(TAG, "This view only supports MATRIX scale type.")
        }
        super.setScaleType(scaleType)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        overlayDrawable.draw(canvas)
    }

    companion object {
        private const val DOUBLE_TAP_ZOOM_DURATION = 200
        private const val DOUBLE_TAP_SCALE_FACTOR = 1.5f
        private const val TAG = "CropImageView"
    }
}
