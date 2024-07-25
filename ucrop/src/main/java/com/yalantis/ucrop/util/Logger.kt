package com.yalantis.ucrop.util

import android.util.Log
import com.yalantis.ucrop.BuildConfig

internal object Logger {
    fun d(tag: String, message: String, vararg args: Any) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, message)
        }
    }

    fun e(tag: String, message: String, e: Throwable) {
        if (BuildConfig.DEBUG) {
            Log.e(tag, message, e)
        }
    }

    fun w(tag: String, message: String, e: Throwable) {
        if (BuildConfig.DEBUG) {
            Log.w(tag, message, e)
        }
    }

    fun w(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.w(tag, message)
        }
    }

    fun i(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.i(tag, message)
        }
    }
}