package com.rittmann.myapplication.main.entity.body

import android.graphics.Rect
import com.rittmann.myapplication.main.entity.Position
import com.rittmann.myapplication.main.extensions.setByPosition
import com.rittmann.myapplication.main.utils.Logger

abstract class PhysicObject(var width: Int, var heigth: Int) : Logger {
    var rect: Rect = Rect(width, heigth, width, heigth)
    var rotationAngle: Double = 0.0

    fun move(position: Position) {
        rect.setByPosition(position, width, heigth)
    }

    fun setRotation(angle: Double) {
        rotationAngle = angle
    }
}