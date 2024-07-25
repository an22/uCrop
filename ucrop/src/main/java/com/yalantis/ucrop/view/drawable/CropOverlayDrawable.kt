package com.yalantis.ucrop.view.drawable

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Point
import android.graphics.Rect
import android.graphics.Region
import android.graphics.drawable.Drawable
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.core.graphics.withSave
import com.yalantis.ucrop.model.CropStyleMode
import com.yalantis.ucrop.util.RectUtils
import com.yalantis.ucrop.view.adapter.KotlinOnGestureListener
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

class CropOverlayDrawable(context: Context) : Drawable() {

    class ElementConfig(
        val color: Int = Color.WHITE,
        val strokeWidth: Float = 2f,
        val isVisible: Boolean = true
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = this@ElementConfig.color
            strokeWidth = this@ElementConfig.strokeWidth
            style = Paint.Style.STROKE
        }

        class Builder(init: (Builder.() -> Unit)? = null) {
            var color: Int = Color.WHITE
            var strokeWidth: Float = 4f
            var isVisible: Boolean = true

            init {
                init?.invoke(this)
            }

            fun build(): ElementConfig {
                return ElementConfig(color, strokeWidth, isVisible)
            }
        }
    }

    enum class CropAreaMode {
        CIRCLE,
        RECTANGLE
    }

    var shouldDrawDim = true
    var shouldDrawOverlay = true
        set(value) {
            field = value
            invalidateSelf()
        }
    var cropAreaMode = CropAreaMode.RECTANGLE
        set(value) {
            field = value
            invalidateSelf()
        }
    var dimColor = Color.TRANSPARENT
        set(value) {
            field = value
            overlayPaint.color = value
            invalidateSelf()
        }
    var cornerSizeInPixels = 40
        set(value) {
            field = value
            invalidateSelf()
        }
    var gridRowCount = 3
        set(value) {
            field = value
            invalidateSelf()
        }
    var gridColumnCount = 3
        set(value) {
            field = value
            invalidateSelf()
        }
    var gridConfig = ElementConfig()
        set(value) {
            field = value
            invalidateSelf()
        }
    var frameConfig = ElementConfig()
        set(value) {
            field = value
            invalidateSelf()
        }
    var cornerConfig = ElementConfig()
        set(value) {
            field = value
            invalidateSelf()
        }
    var aspectRatio = 1f
        set(value) {
            field = value
            invalidateCropArea()
            invalidateSelf()
        }
    var cropRectMinPixelSize = 100
    var touchThresholdPixels = 100f
    var cropStyleMode: CropStyleMode = CropStyleMode.FREESTYLE
    var cropAreaPath = Path()
    var cropAreaRect = Rect()

    private var scrollPointIndexPair = Pair(0, 0)

    private val paddings = intArrayOf(0, 0, 0, 0)
    private val cropRectPadding = Rect()
    private val cornerHackRect = Rect()
    private val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cropAreaPoints = Array(4) { Point() }
    private val gestureDetector = GestureDetector(context, KotlinOnGestureListener(
        onScroll = { event, _, distanceX: Float, distanceY: Float ->
            val nonNullEvent = event ?: return@KotlinOnGestureListener false
            val hash = nonNullEvent.x.hashCode() + nonNullEvent.y.hashCode()
            if (scrollPointIndexPair.first != hash) {
                scrollPointIndexPair = hash to getCurrentTouchIndex(event.x, event.y)
            }
            val nearestPointIndex = scrollPointIndexPair.second
            if (nearestPointIndex == -1) return@KotlinOnGestureListener false
            updateCropBounds(nearestPointIndex, distanceX, distanceY)
            RectUtils.fillPointsFromRect(cropAreaRect, cropAreaPoints)
            invalidateSelf()
            true
        }
    ))

    override fun setBounds(left: Int, top: Int, right: Int, bottom: Int) {
        super.setBounds(left, top, right, bottom)
        invalidateCropArea()
    }

    override fun draw(canvas: Canvas) {
        if (!shouldDrawOverlay) return

        canvas.withSave {
            clipOut()
            drawDim()
        }
        canvas.withSave {
            clipIn()
            drawGrid()
            drawFrame()
            drawCorners()
        }
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        if (cropStyleMode == CropStyleMode.STATIC) return false

        return gestureDetector.onTouchEvent(event)
    }

    fun setCropAreaPadding(paddingLeft:Int, paddingTop:Int, paddingRight:Int, paddingBottom: Int) {
        paddings[0] = paddingLeft
        paddings[1] = paddingTop
        paddings[2] = paddingRight
        paddings[3] = paddingBottom
        invalidateCropArea()
    }

    private fun updateCropBounds(nearestPointIndex: Int, distanceX: Float, distanceY: Float) {
        when (nearestPointIndex) {
            0 -> {
                cropAreaRect.left = max(cropRectPadding.left, cropAreaRect.left - distanceX.toInt())
                cropAreaRect.top = max(cropRectPadding.top, cropAreaRect.top - distanceY.toInt())
            }

            1 -> {
                cropAreaRect.right = min(cropRectPadding.right, cropAreaRect.right - distanceX.toInt())
                cropAreaRect.top = max(cropRectPadding.top, cropAreaRect.top - distanceY.toInt())
            }

            2 -> {
                cropAreaRect.right = min(cropRectPadding.right, cropAreaRect.right - distanceX.toInt())
                cropAreaRect.bottom = min(cropRectPadding.bottom, cropAreaRect.bottom - distanceY.toInt())
            }

            3 -> {
                cropAreaRect.left = max(cropRectPadding.left, cropAreaRect.left - distanceX.toInt())
                cropAreaRect.bottom = min(cropRectPadding.bottom, cropAreaRect.bottom - distanceY.toInt())
            }

            4 -> {
                val newL = max(cropRectPadding.left, cropAreaRect.left - distanceX.toInt())
                val newT = max(cropRectPadding.top, cropAreaRect.top - distanceY.toInt())
                val newR = min(cropRectPadding.right, cropAreaRect.right - distanceX.toInt())
                val newB = min(cropRectPadding.bottom, cropAreaRect.bottom - distanceY.toInt())
                if ((newR - newL) >= cropRectMinPixelSize && (newB - newT) >= cropRectMinPixelSize) {
                    cropAreaRect.set(
                        if (newR != cropAreaRect.right) newL else cropAreaRect.left,
                        if (newB != cropAreaRect.bottom) newT else cropAreaRect.top,
                        if (newL != cropAreaRect.left) newR else cropAreaRect.right,
                        if (newT != cropAreaRect.top) newB else cropAreaRect.bottom
                    )
                }
            }
        }
    }

    private fun invalidateCropArea() {
        invalidatePaddings()
        invalidateAspectRatio()
        invalidateCropAreaPath()
        invalidateSelf()
    }

    private fun invalidatePaddings() {
        cropRectPadding.set(
            bounds.left + paddings[0],
            bounds.top + paddings[1],
            bounds.right - paddings[2],
            bounds.bottom - paddings[3]
        )
    }

    private fun invalidateAspectRatio() {
        val height = (cropRectPadding.width() / aspectRatio).toInt()
        if (height > cropRectPadding.height()) {
            val width: Int = (cropRectPadding.height() * aspectRatio).toInt()
            val halfDiff: Int = (cropRectPadding.width() - width) / 2
            cropAreaRect.set(
                cropRectPadding.left + halfDiff,
                cropRectPadding.top,
                cropRectPadding.right - halfDiff,
                cropRectPadding.bottom
            )
        } else {
            val halfDiff: Int = (cropRectPadding.height() - height) / 2
            cropAreaRect.set(
                cropRectPadding.left,
                cropRectPadding.top + halfDiff,
                cropRectPadding.right,
                cropRectPadding.bottom - halfDiff
            )
        }
        RectUtils.fillPointsFromRect(cropAreaRect, cropAreaPoints)
    }

    private fun invalidateCropAreaPath() {
        cropAreaPath.reset()
        cropAreaPath.addCircle(
            cropAreaRect.centerX().toFloat(),
            cropAreaRect.centerY().toFloat(),
            min(cropAreaRect.width(), cropAreaRect.height()) / 2f,
            Path.Direction.CW
        )
    }

    /**
     * * The order of the corners in the float array is:
     * 0------->1
     * ^        |
     * |   4    |
     * |        v
     * 3<-------2
     *
     * @return - index of corner that is being dragged
     */
    private fun getCurrentTouchIndex(touchX: Float, touchY: Float): Int {
        var index = -1

        for (i in cropAreaPoints.indices) {
            val dist = hypot(touchX - cropAreaPoints[i].x, touchY - cropAreaPoints[i].y)
            if (dist < touchThresholdPixels) {
                index = i
                break
            }
        }

        val canMoveRectBounds = cropStyleMode == CropStyleMode.FREESTYLE_ALLOW_SCROLL_TRANSLATE
        val isTouchInside = cropAreaRect.contains(touchX.toInt(), touchY.toInt())
        if (canMoveRectBounds && isTouchInside && index == -1) {
            index = 4
        }

        return index
    }

    @Suppress("DEPRECATION")
    private fun Canvas.clipOut() {
        when (cropAreaMode) {
            CropAreaMode.CIRCLE -> clipPath(cropAreaPath, Region.Op.DIFFERENCE)
            CropAreaMode.RECTANGLE -> clipRect(cropAreaRect, Region.Op.DIFFERENCE)
        }
    }

    @Suppress("DEPRECATION")
    private fun Canvas.clipIn() {
        when (cropAreaMode) {
            CropAreaMode.CIRCLE -> clipPath(cropAreaPath, Region.Op.INTERSECT)
            CropAreaMode.RECTANGLE -> clipRect(cropAreaRect, Region.Op.INTERSECT)
        }
    }

    private fun Canvas.drawDim() {
        if (!shouldDrawDim) return
        drawColor(dimColor)
        drawPath(cropAreaPath, overlayPaint) // Draw 1px stroke to fix antialias
    }

    private fun Canvas.drawGrid() {
        if (!gridConfig.isVisible) return
        for (i in 1 until gridColumnCount) {
            val x = cropAreaRect.left + (cropAreaRect.width() * i / gridColumnCount.toFloat())
            drawLine(x, cropAreaRect.top.toFloat(), x, cropAreaRect.top + cropAreaRect.height().toFloat(), gridConfig.paint)
        }
        for (i in 1 until gridRowCount) {
            val y = cropAreaRect.top + (cropAreaRect.height() * i / gridRowCount.toFloat())
            drawLine(cropAreaRect.left.toFloat(), y, cropAreaRect.left + cropAreaRect.width().toFloat(), y, gridConfig.paint)
        }
    }

    private fun Canvas.drawFrame() {
        if (!frameConfig.isVisible) return
        drawRect(cropAreaRect, frameConfig.paint)
    }

    @Suppress("DEPRECATION")
    private fun Canvas.drawCorners() {
        if (!cornerConfig.isVisible) return
        cornerHackRect.set(cropAreaRect)
        cornerHackRect.inset(cornerSizeInPixels, -cornerSizeInPixels)
        clipRect(cornerHackRect, Region.Op.DIFFERENCE)

        cornerHackRect.set(cropAreaRect)
        cornerHackRect.inset(-cornerSizeInPixels, cornerSizeInPixels)
        clipRect(cornerHackRect, Region.Op.DIFFERENCE)

        drawRect(cropAreaRect, cornerConfig.paint)
    }

    @Deprecated("Deprecated in Java", ReplaceWith("alpha"))
    override fun getOpacity(): Int {
        return alpha
    }

    override fun setAlpha(alpha: Int) {
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
    }
}