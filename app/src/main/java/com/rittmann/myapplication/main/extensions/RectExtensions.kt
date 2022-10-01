package com.rittmann.myapplication.main.extensions

import android.graphics.Rect
import com.rittmann.myapplication.main.entity.Position

fun Rect.setByPosition(position: Position, width: Int = width(), height: Int = height()) {
    set(
        (position.x.toInt() - width / 2).coerceAtLeast(0),
        (position.y.toInt() - height / 2).coerceAtLeast(0),
        (position.x.toInt() + width / 2).coerceAtLeast(0),
        (position.y.toInt() + height / 2).coerceAtLeast(0),
    )
}

fun Rect.setByPosition(x: Double, y: Double, width: Int = width(), height: Int = height()) {
    set(
        (x.toInt() - width / 2).coerceAtLeast(0),
        (y.toInt() - height / 2).coerceAtLeast(0),
        (x.toInt() + width / 2).coerceAtLeast(0),
        (y.toInt() + height / 2).coerceAtLeast(0),
    )
}