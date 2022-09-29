package com.rittmann.myapplication.main.entity.body

import android.graphics.Rect
import com.rittmann.myapplication.main.entity.Position
import com.rittmann.myapplication.main.extensions.setByPosition

class Body(var width: Int, var heigth: Int) {
    var rect: Rect = Rect(width, heigth, width, heigth)

    fun move(position: Position) {
        rect.setByPosition(position, width, heigth)
    }
}