package com.rittmann.myapplication.main.entity.body

import android.graphics.Rect
import com.rittmann.myapplication.main.entity.Position
import com.rittmann.myapplication.main.extensions.setByPosition
import com.rittmann.myapplication.main.utils.Logger

open class PhysicObject(var width: Int, var height: Int) : Logger {
    var rect: Rect = Rect(width, height, width, height)
    var rotationAngle: Double = 0.0

    fun move(position: Position) {
        rect.setByPosition(position)
    }

    fun setRotation(angle: Double) {
        rotationAngle = angle
    }
}