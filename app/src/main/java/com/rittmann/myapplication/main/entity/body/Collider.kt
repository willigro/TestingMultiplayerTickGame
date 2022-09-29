package com.rittmann.myapplication.main.entity.body

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import com.rittmann.myapplication.main.entity.Position
import com.rittmann.myapplication.main.extensions.setByPosition

class Collider(var body: Body) {
    var position = Position()
    var rect = Rect(body.rect)
    private val paint = Paint()

    init {
//        paint.color = StardardColors.collider
        paint.color = Color.CYAN
    }

    fun isColliding(collider: Collider): Boolean {
        return rect.intersects(collider.rect.left, collider.rect.top, collider.rect.right, collider.rect.bottom)
    }

    fun centralizerVission() {
        position.x -= rect.width() / 2
        position.y -= rect.height() / 2
        move()
    }

    fun contains(x: Int, y: Int): Boolean {
        return rect.contains(x, y)
    }

    fun draw(canvas: Canvas) {
        canvas.drawRect(rect, paint)
    }

    fun move(position: Position = this.position) {
        rect.setByPosition(position)
    }
}
