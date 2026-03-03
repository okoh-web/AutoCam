package com.example.autocam.util

import android.util.Log

object AppLogger {
    private const val TAG = "AutoCam"
    fun i(msg: String) = Log.i(TAG, msg)
    fun e(msg: String, t: Throwable? = null) = Log.e(TAG, msg, t)
}
