package com.rittmann.myapplication.main.entity

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.rittmann.myapplication.main.draw.DrawObject
import com.rittmann.myapplication.main.entity.body.PhysicObjectRect
import com.rittmann.myapplication.main.extensions.setByPosition

private const val POINTER_SIZE = 10

class Pointer(
    position: Position,
    var distanceX: Double,
    var distanceY: Double,
) : PhysicObjectRect(position, POINTER_SIZE, POINTER_SIZE), DrawObject {

    private val paint = Paint()

    init {
        paint.color = Color.YELLOW
    }

    override fun update(deltaTime: Double) {}

    override fun draw(canvas: Canvas) {
        canvas.drawRect(rect, paint)
    }

    override fun free() {}

    override fun setRotation(angle: Double) {
        super.setRotation(angle)

        rotate()
    }

    fun moveAndRotate(x: Double, y: Double) {
        position.sum(x, y)

        rotate()
    }

    fun setMoveAndRotate(x: Double, y: Double) {
        position.set(x, y)

        rotate()
    }

    fun moveAndRotate(position: Position) {
        position.sum(position.x, position.y)

        rotate()
    }

    private fun rotate() {
        getRotatedPosition().apply {
            rect.setByPosition(this.x, this.y, width, height)
        }
    }

    fun getRotatedPosition(angle: Double? = null): Position {
        angle?.also { setRotation(angle) }
        val normalizedPosition = Position.calculateNormalizedPosition(
            rotationAngle
        )

        return position.sumNew(normalizedPosition.multiple(distanceX, distanceY))
    }
}