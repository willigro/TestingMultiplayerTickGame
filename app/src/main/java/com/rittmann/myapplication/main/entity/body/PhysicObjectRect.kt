package com.rittmann.myapplication.main.entity.body

import android.graphics.Rect
import com.rittmann.myapplication.main.entity.Position
import com.rittmann.myapplication.main.extensions.setByPosition
import com.rittmann.myapplication.main.utils.Logger

const val DEFAULT_ROTATION = 360.0

abstract class PhysicObjectRect(var position: Position, var width: Int, var height: Int) : Logger {
    var rect: Rect = Rect(width, height, width, height)
    var rotationAngle: Double = DEFAULT_ROTATION

    open fun move(position: Position) {
        this.position.set(position)
        rect.setByPosition(position, width, height)
    }

    open fun move(x: Double, y: Double) {
        position.set(x, y)
        rect.setByPosition(x, y, width, height)
    }

    open fun setRotation(angle: Double) {
        rotationAngle = angle
    }
}