package com.yalantis.ucrop.view.drawable

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.Region
import android.graphics.drawable.Drawable
import androidx.core.graphics.withSave
import kotlin.math.min

class CropOverlayDrawable : Drawable() {

    class ElementConfig(
        val color: Int = Color.WHITE,
        val strokeWidth: Float = 5f,
        val isVisible: Boolean = true
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = this@ElementConfig.color
            strokeWidth = this@ElementConfig.strokeWidth
            style = Paint.Style.STROKE
        }
    }

    var shouldDrawOverlay = true
    var shouldDrawDim = true
    var cropAreaMode = CropAreaMode.RECTANGLE
    var cropAreaPath = Path()
    var dimColor = Color.TRANSPARENT
    var gridRowCount = 3
    var gridColumnCount = 3
    var gridConfig = ElementConfig()
    var frameConfig = ElementConfig()
    var cornerConfig = ElementConfig()
    var cornerSizeInPixels = 10

    var cropAreaRect = Rect()
    private val tempRect = Rect()

    private val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = dimColor
    }

    enum class CropAreaMode {
        CIRCLE,
        RECTANGLE
    }

    fun setClipAreaBounds(bounds: Rect) {
        cropAreaRect = bounds
        invalidateCropArea()
    }

    override fun setBounds(left: Int, top: Int, right: Int, bottom: Int) {
        super.setBounds(left, top, right, bottom)
        invalidateCropArea()
    }

    private fun invalidateCropArea() {
        cropAreaPath.reset()
        cropAreaPath.addCircle(
            cropAreaRect.centerX().toFloat(),
            cropAreaRect.centerY().toFloat(),
            min(cropAreaRect.width(), cropAreaRect.height()) / 2f,
            Path.Direction.CW
        )
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
        tempRect.set(cropAreaRect)
        tempRect.inset(cornerSizeInPixels, -cornerSizeInPixels)
        clipRect(tempRect, Region.Op.DIFFERENCE)

        tempRect.set(cropAreaRect)
        tempRect.inset(-cornerSizeInPixels, cornerSizeInPixels)
        clipRect(tempRect, Region.Op.DIFFERENCE)

        drawRect(cropAreaRect, frameConfig.paint)
    }

    override fun setAlpha(alpha: Int) {
        TODO("Not yet implemented")
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        TODO("Not yet implemented")
    }

    @Deprecated("Deprecated in Java", ReplaceWith("alpha"))
    override fun getOpacity(): Int {
        return alpha
    }
}