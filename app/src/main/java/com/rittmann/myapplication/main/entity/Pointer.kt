package com.rittmann.myapplication.main.entity

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.rittmann.myapplication.main.draw.DrawObject
import com.rittmann.myapplication.main.entity.body.PhysicObject
import com.rittmann.myapplication.main.extensions.setByPosition

private const val POINTER_SIZE = 10

class Pointer(
    var position: Position = Position(),
    var distanceX: Double,
    var distanceY: Double,
) : PhysicObject(POINTER_SIZE, POINTER_SIZE), DrawObject {

    private val paint = Paint()

    init {
        paint.color = Color.YELLOW
    }

    override fun update() {}

    override fun draw(canvas: Canvas) {
        canvas.drawRect(rect, paint)
    }

    override fun setRotation(angle: Double) {
        super.setRotation(angle)

        rotate()
    }

    fun moveAndRotate(x: Double, y: Double) {
        position.sum(x, y)

        rotate()
    }

    private fun rotate() {
        val normalizedPosition = Position.calculateNormalizedPosition(
            rotationAngle
        )

        position.sumNew(normalizedPosition.multiple(distanceX, distanceY)).apply {
            rect.setByPosition(this.x, this.y, width, heigth)
        }
    }

    fun getRotatedPosition() : Position {
        val normalizedPosition = Position.calculateNormalizedPosition(
            rotationAngle
        )

        return position.sumNew(normalizedPosition.multiple(BODY_WIDTH.toDouble()))
    }
}