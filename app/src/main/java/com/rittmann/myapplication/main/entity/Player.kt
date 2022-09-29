package com.rittmann.myapplication.main.entity

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import com.rittmann.myapplication.main.entity.body.Collider
import com.rittmann.myapplication.main.entity.body.Body
import com.rittmann.myapplication.main.draw.DrawObject
import com.rittmann.myapplication.main.entity.server.PlayerMovementResult
import com.rittmann.myapplication.main.extensions.orZero
import com.rittmann.myapplication.main.match.screen.GLOBAL_TAG

const val BODY_WIDTH = 40
const val BODY_HEIGTH = 40

data class Player(
    val playerId: String = "",
    val position: Position = Position(),
    val color: String = "",
) : DrawObject {

    private val body: Body = Body(BODY_WIDTH, BODY_HEIGTH)
    private val collider: Collider = Collider(body)
    var isMoving: Boolean = false
    var playerMovementResult: PlayerMovementResult? = null

    private val paint = Paint()

    init {
        paint.color = Color.WHITE// Color.parseColor("#FFFFFF")
    }

    override fun update() {
        body.move(position)
    }

    override fun draw(canvas: Canvas) {
        canvas.drawRect(body.rect, paint)
    }

    fun setIsMoving(playerMovementResult: PlayerMovementResult?) {
        this.isMoving = playerMovementResult?.angle.orZero() > 0.0
        this.playerMovementResult = playerMovementResult
        this.playerMovementResult?.newPositionApplied?.set(false)
    }

    fun move(position: Position) {
        val x = position.x * VELOCITY
        val y = position.y * VELOCITY

        this.position.sum(x, y)
    }

    fun setPosition(playerMovementResult: PlayerMovementResult?) {
        playerMovementResult?.newPosition?.also {
            position.set(it.x, it.y)
        }
    }

    companion object {
        const val VELOCITY = 8.0
        private const val VELOCITY_DASH = 8
    }
}
