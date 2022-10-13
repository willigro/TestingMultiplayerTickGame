package com.rittmann.myapplication.main.utils

import android.util.Log
import com.rittmann.myapplication.main.match.screen.GLOBAL_TAG

interface Logger {

    fun String.log(tag: String = javaClass.simpleName, breakLine: Boolean = true) {
//        Thread.currentThread().stackTrace.forEach {
//            Log.i(GLOBAL_TAG, "methodName=${it.methodName}")
//        }
        Log.i(
            tag,
            "$GLOBAL_TAG - ${Thread.currentThread().stackTrace[5].methodName} - ${if (breakLine) "\n" else ""}$this"
        )
    }
}