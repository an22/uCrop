package com.yalantis.ucrop.util

import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.graphics.values
import com.yalantis.ucrop.gesture.RotationGestureDetector
import com.yalantis.ucrop.view.adapter.KotlinOnGestureListener
import com.yalantis.ucrop.view.adapter.KotlinOnRotateListener
import com.yalantis.ucrop.view.adapter.KotlinOnScaleListener
import kotlin.math.pow

internal class ImageGestureManager(
    context: Context,
    private val drawMatrix: Matrix,
    private val matrixAnimator: MatrixAnimator,
    private val onImageMatrixChanged: (Matrix) -> Unit
) {
    private val mScaleDetector = ScaleGestureDetector(context, KotlinOnScaleListener(onScale = {
        drawMatrix.postScale(it.scaleFactor, it.scaleFactor, it.focusX, it.focusY)
        onImageMatrixChanged(drawMatrix)
        true
    }))
    private val mRotateDetector = RotationGestureDetector(KotlinOnRotateListener(onRotation = {
        currentRotationAngle += it.angle
        drawMatrix.postRotate(it.angle, it.touchCenter.x, it.touchCenter.y)
        onImageMatrixChanged(drawMatrix)
        true
    }))
    private val mGestureDetector = GestureDetector(context, KotlinOnGestureListener(
        onScroll = { _, _, distanceX: Float, distanceY: Float ->
            drawMatrix.postTranslate(-distanceX, -distanceY)
            onImageMatrixChanged(drawMatrix)
            true
        },
        onDoubleTap = {
            val currentScale = drawMatrix.values()[Matrix.MSCALE_X]
            val scaleFactor = (scaleRange.endInclusive / scaleRange.start).pow((1.0f / doubleTapScaleSteps))
            val newScale = currentScale + scaleFactor
            if (newScale !in scaleRange) return@KotlinOnGestureListener false
            matrixAnimator.animate(
                matrix = drawMatrix,
                scalePoint = PointF(it.x, it.y),
                translateDelta = PointF(0f, 0f),
                scaleOn = scaleFactor,
                durationMs = DOUBLE_TAP_ZOOM_DURATION,
                interpolator = AccelerateDecelerateInterpolator()
            ) { newMatrix ->
                drawMatrix.set(newMatrix)
                onImageMatrixChanged(drawMatrix)
            }
            true
        }
    ), null, true)

    var currentRotationAngle = 0f
        private set

    var doubleTapScaleSteps: Int = 5
    var isRotateEnabled: Boolean = true
    var isScaleEnabled: Boolean = true
    var isGestureEnabled: Boolean = true

    var doubleTapZoomDuration = DOUBLE_TAP_ZOOM_DURATION
    var maxScaleFactor = DOUBLE_TAP_MAX_SCALE
        set(value) {
            field = value
            invalidateScaleRange()
        }
    private var scaleRange = 1f..2f

    fun onTouchEvent(event: MotionEvent): Boolean {
        if (isGestureEnabled) {
            mGestureDetector.onTouchEvent(event)
        }

        if (isScaleEnabled) {
            mScaleDetector.onTouchEvent(event)
        }

        if (isRotateEnabled) {
            mRotateDetector.onTouchEvent(event)
        }
        return true
    }

    fun invalidateScaleRange() {
        val minScale = drawMatrix.values()[Matrix.MSCALE_X]
        val maxScale = minScale * maxScaleFactor

        scaleRange = minScale..maxScale
    }

    companion object {
        private const val DOUBLE_TAP_ZOOM_DURATION = 200L
        private const val DOUBLE_TAP_MAX_SCALE = 10f
    }
}