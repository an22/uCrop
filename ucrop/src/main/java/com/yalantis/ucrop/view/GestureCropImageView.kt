package com.yalantis.ucrop.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.graphics.toRectF
import com.yalantis.ucrop.model.CropStyleMode
import com.yalantis.ucrop.util.ImageGestureManager
import com.yalantis.ucrop.util.Logger
import com.yalantis.ucrop.util.MatrixAnimator
import com.yalantis.ucrop.util.RectangleAligner
import com.yalantis.ucrop.view.drawable.CropOverlayDrawable
import com.yalantis.ucrop.view.drawable.CropOverlayDrawable.ElementConfig

open class GestureCropImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : AppCompatImageView(context, attrs, defStyle) {

    private val drawMatrix = Matrix()
    private var bitmapSourceBounds = FloatArray(8)
    private val bitmapActualBounds = FloatArray(8)
    private val overlayDrawable = CropOverlayDrawable(context)
    private val rectangleAligner = RectangleAligner()
    private val matrixAnimator = MatrixAnimator()
    private val imageGestureManager = ImageGestureManager(
        context = context,
        drawMatrix = drawMatrix,
        matrixAnimator = matrixAnimator,
        onImageMatrixChanged = ::setImageMatrix
    )

    var isRotateEnabled: Boolean by imageGestureManager::isRotateEnabled
    var isScaleEnabled: Boolean by imageGestureManager::isScaleEnabled
    var isGestureEnabled: Boolean by imageGestureManager::isGestureEnabled
    var imageToCropBoundsAnimDuration: Long = DEFAULT_IMAGE_TO_CROP_BOUNDS_ANIM_DURATION
    var maxScaleMultiplier: Float by imageGestureManager::maxScaleFactor
    var doubleTapAnimDuration: Long by imageGestureManager::doubleTapZoomDuration
    var dimColor: Int by overlayDrawable::dimColor
    var cropAreaMode: CropOverlayDrawable.CropAreaMode by overlayDrawable::cropAreaMode
    var shouldDrawOverlay: Boolean by overlayDrawable::shouldDrawOverlay
    var gridColumnCount:Int by overlayDrawable::gridColumnCount
    var gridRowCount:Int by overlayDrawable::gridRowCount
    var frameConfig: ElementConfig by overlayDrawable::frameConfig
    var frameCornerConfig: ElementConfig by overlayDrawable::cornerConfig
    var frameGridConfig: ElementConfig by overlayDrawable::gridConfig
    var aspectRatio: Float by overlayDrawable::aspectRatio
    var isFreestyleCrop: Boolean
        get() = overlayDrawable.cropStyleMode == CropStyleMode.FREESTYLE
        set(value) {
            overlayDrawable.cropStyleMode = when(value) {
                true -> CropStyleMode.FREESTYLE_ALLOW_SCROLL_TRANSLATE
                else -> CropStyleMode.STATIC
            }
        }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        overlayDrawable.callback = this
    }

    override fun onDetachedFromWindow() {
        overlayDrawable.callback = null
        super.onDetachedFromWindow()
    }

    override fun invalidateDrawable(dr: Drawable) {
        if (dr == overlayDrawable) {
            invalidate()
        } else {
            super.invalidateDrawable(dr)
        }
    }

    override fun setImageBitmap(bm: Bitmap?) {
        scaleType = ScaleType.FIT_CENTER
        super.setImageBitmap(bm)
        scaleType = ScaleType.MATRIX
        drawMatrix.set(imageMatrix)
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

        wrapCropBounds(animate = false)
        imageGestureManager.invalidateScaleRange()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        overlayDrawable.setBounds(paddingLeft, paddingTop, w - paddingRight, h - paddingBottom)
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
        when ((event.action and MotionEvent.ACTION_MASK)) {
            MotionEvent.ACTION_DOWN -> matrixAnimator.cancelAnimations()
            MotionEvent.ACTION_UP -> wrapCropBounds(animate = true)
        }
        if(!overlayDrawable.onTouchEvent(event)) {
            imageGestureManager.onTouchEvent(event)
        }
        return true
    }

    private fun wrapCropBounds(animate: Boolean) {
        val result = rectangleAligner.align(
            outRect = bitmapActualBounds,
            inRect = overlayDrawable.cropAreaRect,
            sourceRotationAngle = imageGestureManager.currentRotationAngle
        )
        if (result.transformationMatrix.isIdentity) return
        if (animate) {
            val bounds = overlayDrawable.cropAreaRect.toRectF()
            val scalePointF = PointF(bounds.centerX(), bounds.centerY())
            matrixAnimator.animate(
                matrix = drawMatrix,
                scalePoint = scalePointF,
                translateDelta = result.translate,
                scaleOn = result.scale,
                interpolator = AccelerateDecelerateInterpolator(),
                durationMs = DEFAULT_IMAGE_TO_CROP_BOUNDS_ANIM_DURATION
            ) {
                drawMatrix.set(it)
                imageMatrix = drawMatrix
            }
        } else {
            drawMatrix.postConcat(result.transformationMatrix)
            imageMatrix = drawMatrix
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
        private const val DEFAULT_IMAGE_TO_CROP_BOUNDS_ANIM_DURATION = 500L
        private const val TAG = "CropImageView"
    }
}
