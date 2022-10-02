package com.rittmann.myapplication.main.entity.collisor

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.rittmann.myapplication.main.draw.DrawObject
import com.rittmann.myapplication.main.entity.Position
import com.rittmann.myapplication.main.entity.body.PhysicObjectRect

class Collider(
    position: Position,
    width: Int,
    height: Int,
    private val father: Collidable,
) : PhysicObjectRect(position, width, height), DrawObject {

    /*
    * When true, this collider will not receive new notifications of collision
    * */
    private var locked: Boolean = false

    private val paint: Paint = Paint()

    init {
        paint.color = Color.CYAN
    }

    fun enable() {
        GlobalCollisions.add(father)
    }

    fun disable() {
        GlobalCollisions.remove(father)
    }

    fun isColliding(collider: Collider): Boolean {
        return !(collider.rect.left > rect.right ||
                collider.rect.right < rect.left ||
                collider.rect.top > rect.bottom ||
                collider.rect.bottom < rect.top)
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

    override fun free() {
        disable()
    }

    fun isLocked(): Boolean = locked
}
