package com.yalantis.ucrop.view.adapter

import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.OnScaleGestureListener

class KotlinOnScaleListener(
    private val onScale: ((ScaleGestureDetector) -> Boolean)? = null,
    private val onScaleBegin: ((ScaleGestureDetector) -> Boolean)? = null,
    private val onScaleEnd: ((ScaleGestureDetector) -> Unit)? = null
) : OnScaleGestureListener {
    override fun onScale(p0: ScaleGestureDetector): Boolean {
        return onScale?.invoke(p0) ?: false
    }

    override fun onScaleBegin(p0: ScaleGestureDetector): Boolean {
        return onScaleBegin?.invoke(p0) ?: true
    }

    override fun onScaleEnd(p0: ScaleGestureDetector) {
        onScaleEnd?.invoke(p0)
    }
}