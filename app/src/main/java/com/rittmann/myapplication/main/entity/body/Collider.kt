package com.rittmann.myapplication.main.entity.body

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.rittmann.myapplication.main.draw.DrawObject

class Collider(width: Int, height: Int) : PhysicObject(width, height), DrawObject {

    /*
    * When true, this collider will not receive new notifications of collision
    * */
    private var locked: Boolean = false

    private val paint: Paint = Paint()

    init {
        paint.color = Color.CYAN
    }

    fun isColliding(collider: Collider): Boolean {
        return rect.intersects(
            collider.rect.left,
            collider.rect.top,
            collider.rect.right,
            collider.rect.bottom
        )
    }

    fun contains(x: Int, y: Int): Boolean {
        return rect.contains(x, y)
    }

    override fun update() {
        TODO("Not yet implemented")
    }

    override fun draw(canvas: Canvas) {
        canvas.drawRect(rect, paint)
    }

    fun isLocked(): Boolean = locked
}
