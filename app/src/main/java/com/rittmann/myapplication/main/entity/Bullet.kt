package com.rittmann.myapplication.main.entity

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.rittmann.myapplication.main.draw.DrawObject
import com.rittmann.myapplication.main.entity.body.Body
import com.rittmann.myapplication.main.entity.collisor.Collidable
import com.rittmann.myapplication.main.entity.collisor.Collider
import com.rittmann.myapplication.main.utils.Logger

private const val BULLET_SIZE = 15
const val BULLET_VELOCITY = 10.0

class Bullet(
    val bulletId: String,
    val position: Position,
    val angle: Double,
) : DrawObject, Collidable, Logger {

    private var initialPosition: Position = position.copy()
    private val body: Body = Body(position.copy(), BULLET_SIZE, BULLET_SIZE).apply {
        setRotation(angle)
    }
    private val collider: Collider = Collider(position.copy(), BULLET_SIZE, BULLET_SIZE, this)
    private val paint: Paint = Paint()

    init {
        paint.color = Color.RED
    }

    override fun update() {
        val normalizedPosition = Position.calculateNormalizedPosition(
            angle
        )

        val x = normalizedPosition.x * BULLET_VELOCITY
        val y = normalizedPosition.y * BULLET_VELOCITY

        position.sum(x, y)

        body.move(position)
        collider.move(position)
    }

    override fun draw(canvas: Canvas) {
        canvas.drawRect(body.rect, paint)
    }

    override fun free() {
        retrieveCollider().free()
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

    fun isFree(): Boolean = initialPosition.distance(position) >= 200.0
}