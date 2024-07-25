package com.yalantis.ucrop.view.adapter

import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent

class KotlinOnGestureListener(
    private val onScroll: ((MotionEvent?, MotionEvent, Float, Float) -> Boolean)? = null,
    private val onDoubleTap: ((MotionEvent) -> Boolean)? = null
) : SimpleOnGestureListener() {

    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        return onScroll?.invoke(e1, e2, distanceX, distanceY)?: super.onScroll(e1, e2, distanceX, distanceY)
    }

    override fun onDoubleTap(e: MotionEvent): Boolean {
        return onDoubleTap?.invoke(e) ?: super.onDoubleTap(e)
    }
}