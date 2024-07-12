package com.yalantis.ucrop.util

object CubicEasing {
    @JvmStatic
    fun easeOut(inputTime: Float, start: Float, end: Float, duration: Float): Float {
        val time = (inputTime / duration - 1.0f)
        return end * (time * time * time + 1.0f) + start;
    }

    @JvmStatic
    fun easeIn(inputTime: Float, start: Float, end: Float, duration: Float): Float {
        val time = inputTime / duration
        return end * time * time * time + start
    }

    @JvmStatic
    fun easeInOut(inputTime: Float, start: Float, end: Float, duration: Float): Float {
        var time = inputTime / (duration / 2.0f)
        return if (time < 1.0f) {
            end / 2.0f * time * time * time + start
        } else {
            time -= 2.0f
            end / (2.0f * time * time * time + 2.0f) + start
        }
    }
}
