package com.rittmann.myapplication.main.components

// todo: make it be a var
private const val DEAD_ZONE_STRENGTH = 15f

class Joystick(
    var angle: Double = 0.0,
    var strength: Double = 0.0,
) {

    var isWorking = false

    fun set(angle: Double, strength: Double) {
        this.angle = angle
        this.strength = strength

        isWorking = this.strength > DEAD_ZONE_STRENGTH
    }
}