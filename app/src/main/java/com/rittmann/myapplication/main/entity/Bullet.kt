package com.rittmann.myapplication.main.entity

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.rittmann.myapplication.main.draw.DrawObject
import com.rittmann.myapplication.main.entity.body.Body
import com.rittmann.myapplication.main.entity.collisor.Collidable
import com.rittmann.myapplication.main.entity.collisor.Collider
import com.rittmann.myapplication.main.scene.SceneManager
import com.rittmann.myapplication.main.utils.Logger

private const val BULLET_SIZE = 15
const val BULLET_DEFAULT_VELOCITY = 500.0
const val BULLET_DEFAULT_MAX_DISTANCE = 1000.0

data class Bullet(
    val bulletId: String,
    var ownerId: String,
    val position: Position,
    var angle: Double,
    var maxDistance: Double,
    var velocity: Double,
) : DrawObject, Collidable, Logger {

    var initialPosition: Position = position.copy()
    private val body: Body = Body(position.copy(), BULLET_SIZE, BULLET_SIZE).apply {
        setRotation(angle)
    }
    private val collider: Collider = Collider(position.copy(), BULLET_SIZE, BULLET_SIZE, this)
    private val paint: Paint = Paint()

    init {
        paint.color = Color.RED
    }

    override fun update(deltaTime: Double) {
        val normalizedPosition = Position.calculateNormalizedPosition(
            angle
        )

        val preX = position.x
        val preY = position.y

        val x = normalizedPosition.x * velocity * deltaTime
        val y = normalizedPosition.y * velocity * deltaTime

        position.sum(x, y)

        ("Tick " + SceneManager.tickNumberBeenProcessed +
                " From=" + SceneManager.processState +
                " Bullet=" + bulletId +
//                " Angle=" + angle +
//                " Velocity=" + velocity +
                " Pre X=" + preX +
                " Pre Y=" + preY +
                " New X=" + position.x +
                " New Y=" + position.y + " bulletMoving").log()

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

    fun isFree(deltaTime: Double): Boolean = initialPosition.distance(position).apply {
//        ("distance=" + this.toString() +
//                ", initialPosition=" + initialPosition.toString() +
//                ", position=" + position.toString() +
//                ", angle=" + angle.toString() +
//                ", deltaTime=" + deltaTime.toString()
//                ).log()
    } >= maxDistance

    fun updateValues(bullet: Bullet) {
        position.set(bullet.position)
        angle = bullet.angle
        body.setRotation(angle)
        maxDistance = bullet.maxDistance
        velocity = bullet.velocity
    }

    companion object {
        fun build(bullet: Bullet) = Bullet(
            bulletId = bullet.bulletId,
            ownerId = bullet.ownerId,
            position = Position(
                x = bullet.position.x,
                y = bullet.position.y
            ),
            angle = bullet.angle,
            velocity = BULLET_DEFAULT_VELOCITY,
            maxDistance = BULLET_DEFAULT_MAX_DISTANCE,
        ).apply {
            initialPosition = bullet.initialPosition
        }
    }
}