package com.yalantis.ucrop.util

import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import androidx.core.graphics.toRectF
import com.yalantis.ucrop.util.RectUtils.getCornersFromRect
import com.yalantis.ucrop.util.RectUtils.getRectSidesFromCorners
import com.yalantis.ucrop.util.RectUtils.trapToRect
import kotlin.math.max

internal class RectangleAligner {

    private val workMatrix = Matrix()
    private val preAllocatedFloatArray = FloatArray(4)

    fun align(
        outRect: FloatArray,
        inRect: Rect,
        sourceRotationAngle: Float
    ): AlignResult {
        if (isPointsInsideRect(outRect, inRect)) return AlignResult(
            transformationMatrix = Matrix(),
            translate = PointF(0f,0f),
            scale = 1f
        )
        val imageCenterX = (outRect[0] + outRect[4]) / 2f
        val imageCenterY = (outRect[1] + outRect[5]) / 2f

        val inRectF = inRect.toRectF()
        var deltaX = inRect.centerX() - imageCenterX
        var deltaY = inRect.centerY() - imageCenterY

        val tempRectCorners = outRect.copyOf()
        workMatrix.reset()
        workMatrix.setTranslate(deltaX, deltaY)
        workMatrix.mapPoints(tempRectCorners)

        val isTranslateSufficient =
            isImageWrapCropBounds(tempRectCorners, inRectF, sourceRotationAngle)

        var scale = 1f
        if (isTranslateSufficient) {
            val imageIndents = calculateImageIndents(outRect, inRectF, sourceRotationAngle)
            deltaX = -(imageIndents[0] + imageIndents[2])
            deltaY = -(imageIndents[1] + imageIndents[3])
        } else {
            workMatrix.reset()
            workMatrix.setRotate(sourceRotationAngle)
            workMatrix.mapRect(inRectF)

            val currentImageSides = getRectSidesFromCorners(outRect)

            scale = max(
                (inRectF.width() / currentImageSides[0]),
                (inRectF.height() / currentImageSides[1])
            )
        }

        workMatrix.reset()
        workMatrix.postTranslate(deltaX, deltaY)
        if (!isTranslateSufficient) {
            workMatrix.postScale(scale, scale, inRectF.centerX(), inRectF.centerY())
        }
        return AlignResult(
            transformationMatrix = Matrix(workMatrix),
            translate = PointF(deltaX, deltaY),
            scale = scale
        )
    }

    /**
     * This methods checks whether a rectangle that is represented as 4 corner points (8 floats)
     * fills the crop bounds rectangle.
     *
     * @param imageCorners - corners of a rectangle
     * @return - true if it wraps crop bounds, false - otherwise
     */
    private fun isImageWrapCropBounds(
        outRect: FloatArray,
        inRectF: RectF,
        rotationAngle: Float
    ): Boolean {
        workMatrix.reset()
        workMatrix.setRotate(-rotationAngle)

        val unrotatedImageCorners = outRect.copyOf()
        workMatrix.mapPoints(unrotatedImageCorners)

        val unrotatedCropBoundsCorners = getCornersFromRect(inRectF)
        workMatrix.mapPoints(unrotatedCropBoundsCorners)

        return trapToRect(unrotatedImageCorners).contains(trapToRect(unrotatedCropBoundsCorners))
    }

    /**
     * This methods checks whether a rectangle that is represented as 4 corner points (8 floats)
     * fills the crop bounds rectangle.
     *
     * @param outRect - corners of an out rectangle
     * @param inRect - RectF of a rectangle that expected to be inside outRect
     * @return - true if it wraps crop bounds, false - otherwise
     */
    fun isPointsInsideRect(outRect: FloatArray, inRect: Rect): Boolean {
        assert(outRect.size == 8)
        for (i in outRect.indices step 2) {
            val x = outRect[i]
            val y = outRect[i + 1]

            if (!inRect.contains(x.toInt(), y.toInt())) {
                return false
            }
        }
        return true
    }

    /**
     * First, un-rotate image and crop rectangles (make image rectangle axis-aligned).
     * Second, calculate deltas between those rectangles sides.
     * Third, depending on delta (its sign) put them or zero inside an array.
     * Fourth, using Matrix, rotate back those points (indents).
     *
     * @return [FloatArray]- array of image indents (4 floats) - in this order [left, top, right, bottom]
     */
    private fun calculateImageIndents(
        outRect: FloatArray,
        inRect: RectF,
        rotationAngle: Float
    ): FloatArray {
        workMatrix.reset()
        workMatrix.setRotate(-rotationAngle)

        val unrotatedImageCorners: FloatArray = outRect.copyOf()
        val unrotatedCropBoundsCorners = getCornersFromRect(inRect)

        workMatrix.mapPoints(unrotatedImageCorners)
        workMatrix.mapPoints(unrotatedCropBoundsCorners)

        val unrotatedImageRect = trapToRect(unrotatedImageCorners)
        val unrotatedCropRect = trapToRect(unrotatedCropBoundsCorners)

        val deltaLeft = unrotatedImageRect.left - unrotatedCropRect.left
        val deltaTop = unrotatedImageRect.top - unrotatedCropRect.top
        val deltaRight = unrotatedImageRect.right - unrotatedCropRect.right
        val deltaBottom = unrotatedImageRect.bottom - unrotatedCropRect.bottom

        val indents = preAllocatedFloatArray
        indents[0] = if ((deltaLeft > 0)) deltaLeft else 0f
        indents[1] = if ((deltaTop > 0)) deltaTop else 0f
        indents[2] = if ((deltaRight < 0)) deltaRight else 0f
        indents[3] = if ((deltaBottom < 0)) deltaBottom else 0f

        workMatrix.reset()
        workMatrix.setRotate(rotationAngle)
        workMatrix.mapPoints(indents)

        return indents
    }

    data class AlignResult(
        val transformationMatrix: Matrix,
        val translate: PointF,
        val scale: Float

    )
}