package com.yalantis.ucrop.view.adapter

import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent

class KotlinOnGestureListener(
    private val onScroll: ((MotionEvent?, MotionEvent, Float, Float) -> Unit)? = null,
    private val onDoubleTap: ((MotionEvent) -> Unit)? = null
) : SimpleOnGestureListener() {

    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        onScroll?.invoke(e1, e2, distanceX, distanceY)
        return super.onScroll(e1, e2, distanceX, distanceY)
    }

    override fun onDoubleTap(e: MotionEvent): Boolean {
        onDoubleTap?.invoke(e)
        return super.onDoubleTap(e)
    }
}