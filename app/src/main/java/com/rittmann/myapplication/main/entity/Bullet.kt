package com.rittmann.myapplication.main.entity

import android.graphics.Canvas
import com.rittmann.myapplication.main.draw.DrawObject
import com.rittmann.myapplication.main.entity.body.Body
import com.rittmann.myapplication.main.entity.body.Collider
import com.rittmann.myapplication.main.utils.Logger

class Bullet(
    val bulletId: String,
    val position: Position = Position(),
) : DrawObject, Collidable, Logger {

    private val body: Body = Body(BODY_WIDTH, BODY_HEIGHT)
    private val collider: Collider = Collider(BODY_WIDTH, BODY_HEIGHT)

    override fun update() {

    }

    override fun draw(canvas: Canvas) {
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