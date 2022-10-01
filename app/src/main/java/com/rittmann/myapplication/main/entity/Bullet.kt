package com.rittmann.myapplication.main.entity

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.rittmann.myapplication.main.draw.DrawObject
import com.rittmann.myapplication.main.entity.body.Body
import com.rittmann.myapplication.main.entity.body.Collider
import com.rittmann.myapplication.main.utils.Logger

private const val BULLET_SIZE = 15

class Bullet(
    val bulletId: String,
    val position: Position,
    val angle: Double,
) : DrawObject, Collidable, Logger {

    private val body: Body = Body(BULLET_SIZE, BULLET_SIZE).apply {
        setRotation(angle)
    }
    private val collider: Collider = Collider(BULLET_SIZE, BULLET_SIZE)
    private val paint: Paint = Paint()
    private val velocity = 10.0

    init {
        paint.color = Color.RED
    }


    override fun update() {
        val normalizedPosition = Position.calculateNormalizedPosition(
            angle
        )

        val x = normalizedPosition.x * velocity
        val y = normalizedPosition.y * velocity

        position.sum(x, y)

        body.move(position)
    }

    override fun draw(canvas: Canvas) {
        canvas.drawRect(body.rect, paint)
    }

    override fun retrieveCollider(): Collider {
        return collider
    }

    override fun collidingWith(collidable: Collidable) {
        when (collidable) {
            is Bullet -> {
                "Bullet $bulletId is Touching a bullet".log()
            }
            is Player -> {
                "Bullet $bulletId is Touching a player".log()
            }
        }
    }
}