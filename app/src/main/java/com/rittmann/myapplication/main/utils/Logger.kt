package com.rittmann.myapplication.main.utils

import android.util.Log
import com.rittmann.myapplication.main.match.screen.GLOBAL_TAG

interface Logger {

    fun String.log(tag: String = javaClass.simpleName) {
        Log.i(tag, "$GLOBAL_TAG - $this")
    }
}