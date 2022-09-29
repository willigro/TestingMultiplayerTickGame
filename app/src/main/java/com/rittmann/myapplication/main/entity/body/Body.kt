package com.rittmann.myapplication.main.entity.body

import android.graphics.Rect
import com.rittmann.myapplication.main.entity.Position

class Body(var width: Int, var heigth: Int) {
    var rect: Rect = Rect(width, heigth, width, heigth)

    fun move(position: Position) {
        rect.set(
            (position.x.toInt() - width / 2).coerceAtLeast(0),
            (position.y.toInt() - heigth / 2).coerceAtLeast(0),
            (position.x.toInt() + width / 2).coerceAtLeast(0),
            (position.y.toInt() + heigth / 2).coerceAtLeast(0),
        )
    }
}