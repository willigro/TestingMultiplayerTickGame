package com.rittmann.myapplication.main.components

class Joystick(
    var angle: Double = 0.0,
    var strength: Double = 0.0,
) {

    var isWorking = false

    fun set(angle: Double, strength: Double) {
        this.angle = angle
        this.strength = strength

        isWorking = this.angle > 0.0
    }
}