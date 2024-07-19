package com.yalantis.ucrop.view.adapter

import com.yalantis.ucrop.gesture.RotationGestureDetector
import com.yalantis.ucrop.gesture.RotationGestureDetector.OnRotationGestureListener

class KotlinOnRotateListener(
    private val onRotation: ((RotationGestureDetector) -> Boolean)? = null
) : OnRotationGestureListener {
    override fun onRotation(rotationDetector: RotationGestureDetector): Boolean {
        return onRotation?.invoke(rotationDetector) ?: false
    }
}