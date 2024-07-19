package com.yalantis.ucrop.gesture

import android.view.MotionEvent
import java.lang.Math.toDegrees
import kotlin.math.atan2

class RotationGestureDetector(
    private val mListener: OnRotationGestureListener?
) {
    private var fX = 0f
    private var fY = 0f
    private var sX = 0f
    private var sY = 0f

    private var pointerIndex1: Int
    private var pointerIndex2: Int
    private var isFirstTouch = false
    var angle: Float = 0f
        private set

    init {
        pointerIndex1 = INVALID_POINTER_INDEX
        pointerIndex2 = INVALID_POINTER_INDEX
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                sX = event.x
                sY = event.y
                pointerIndex1 = event.findPointerIndex(event.getPointerId(0))
                angle = 0f
                isFirstTouch = true
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                fX = event.x
                fY = event.y
                pointerIndex2 = event.findPointerIndex(event.getPointerId(event.actionIndex))
                angle = 0f
                isFirstTouch = true
            }

            MotionEvent.ACTION_MOVE -> if (pointerIndex1 != INVALID_POINTER_INDEX && pointerIndex2 != INVALID_POINTER_INDEX && event.pointerCount > pointerIndex2) {
                val nsX = event.getX(pointerIndex1)
                val nsY = event.getY(pointerIndex1)
                val nfX = event.getX(pointerIndex2)
                val nfY = event.getY(pointerIndex2)

                if (isFirstTouch) {
                    angle = 0f
                    isFirstTouch = false
                } else {
                    calculateAngleBetweenLines(fX, fY, sX, sY, nfX, nfY, nsX, nsY)
                }

                mListener?.onRotation(this)
                fX = nfX
                fY = nfY
                sX = nsX
                sY = nsY
            }

            MotionEvent.ACTION_UP -> pointerIndex1 = INVALID_POINTER_INDEX
            MotionEvent.ACTION_POINTER_UP -> pointerIndex2 = INVALID_POINTER_INDEX
        }
        return true
    }

    private fun calculateAngleBetweenLines(
        fx1: Float, fy1: Float, fx2: Float, fy2: Float,
        sx1: Float, sy1: Float, sx2: Float, sy2: Float
    ): Float {
        return calculateAngleDelta(
            toDegrees(
                atan2((fy1 - fy2).toDouble(), (fx1 - fx2).toDouble())
                    .toFloat().toDouble()
            ).toFloat(),
            toDegrees(
                atan2((sy1 - sy2).toDouble(), (sx1 - sx2).toDouble())
                    .toFloat().toDouble()
            ).toFloat()
        )
    }

    private fun calculateAngleDelta(angleFrom: Float, angleTo: Float): Float {
        angle = angleTo % 360.0f - angleFrom % 360.0f

        if (angle < -180.0f) {
            angle += 360.0f
        } else if (angle > 180.0f) {
            angle -= 360.0f
        }

        return angle
    }

    interface OnRotationGestureListener {
        fun onRotation(rotationDetector: RotationGestureDetector): Boolean
    }

    companion object {
        private const val INVALID_POINTER_INDEX = -1
    }
}