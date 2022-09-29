package com.rittmann.myapplication.main.extensions

import android.graphics.Rect
import com.rittmann.myapplication.main.entity.Position

fun Rect.setByPosition(position: Position, width: Int, heigth: Int) {
    set(
        (position.x.toInt() - width / 2).coerceAtLeast(0),
        (position.y.toInt() - heigth / 2).coerceAtLeast(0),
        (position.x.toInt() + width / 2).coerceAtLeast(0),
        (position.y.toInt() + heigth / 2).coerceAtLeast(0),
    )
}