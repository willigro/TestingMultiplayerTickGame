package com.rittmann.myapplication.main.entity.body

import android.graphics.Rect
import com.rittmann.myapplication.main.entity.Position
import com.rittmann.myapplication.main.extensions.setByPosition
import com.rittmann.myapplication.main.utils.Logger

const val DEFAULT_ROTATION = 360.0

abstract class PhysicObject(var width: Int, var heigth: Int) : Logger {
    var rect: Rect = Rect(width, heigth, width, heigth)
    var rotationAngle: Double = DEFAULT_ROTATION

    open fun move(position: Position) {
        rect.setByPosition(position, width, heigth)
    }

    open fun move(x: Double, y: Double) {
        rect.setByPosition(x, y, width, heigth)
    }

    open fun setRotation(angle: Double) {
        rotationAngle = angle
    }
}