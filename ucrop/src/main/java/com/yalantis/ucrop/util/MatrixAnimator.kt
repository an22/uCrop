package com.yalantis.ucrop.util

import android.animation.Animator
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.graphics.Matrix
import android.graphics.PointF

internal class MatrixAnimator {

    private var currentAnimator:Animator? = null

    fun animate(
        matrix: Matrix,
        scalePoint: PointF,
        translateDelta: PointF,
        scaleOn: Float,
        durationMs: Long,
        interpolator: TimeInterpolator,
        onTransformationMatrixReady:(Matrix) -> Unit
    ) {
        cancelAnimations()
        val sourceMatrix = Matrix(matrix)
        val transformationMatrix = Matrix()
        val sourceCopy = Matrix()
        currentAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = durationMs
            this.interpolator = interpolator
            addUpdateListener {
                val scale = 1f + (scaleOn - 1f) * it.animatedFraction //1f - respect source scale
                val translateX = translateDelta.x * it.animatedFraction
                val translateY = translateDelta.y * it.animatedFraction
                sourceCopy.set(sourceMatrix)
                transformationMatrix.reset()
                transformationMatrix.postTranslate(translateX, translateY)
                transformationMatrix.postScale(
                    scale,
                    scale,
                    scalePoint.x,
                    scalePoint.y
                )
                sourceCopy.postConcat(transformationMatrix)
                onTransformationMatrixReady(sourceCopy)
            }
            start()
        }
    }

    fun cancelAnimations() {
        currentAnimator?.cancel()
        currentAnimator = null
    }
}