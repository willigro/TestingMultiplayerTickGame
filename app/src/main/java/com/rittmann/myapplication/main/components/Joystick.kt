package com.rittmann.myapplication.main.components

import android.util.Log
import com.rittmann.myapplication.main.match.screen.GLOBAL_TAG

class Joystick(
    var angle: Double = 0.0,
    var strength: Double = 0.0,
) {

    var isWorking = false

    fun set(angle: Double, strength: Double) {
//        Log.i(GLOBAL_TAG, "set - angle=$angle")
        this.angle = angle
        this.strength = strength

        isWorking = this.angle > 0.0
    }
}