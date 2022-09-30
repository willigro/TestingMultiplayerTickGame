package com.rittmann.myapplication.main.entity

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.rittmann.myapplication.main.draw.DrawObject
import com.rittmann.myapplication.main.entity.body.Body
import com.rittmann.myapplication.main.entity.body.Collider
import com.rittmann.myapplication.main.entity.server.PlayerMovementResult
import com.rittmann.myapplication.main.extensions.orZero
import com.rittmann.myapplication.main.match.screen.MatchActivity
import kotlin.math.cos
import kotlin.math.sin

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
        paint.color = Color.parseColor(color.ifEmpty { "#FFFFFF" })
    }

    override fun update() {
        body.move(position)
    }

    override fun draw(canvas: Canvas) {
        canvas.drawRect(body.rect, paint)
    }

    fun keepTheNextPlayerMovement(playerMovementResult: PlayerMovementResult?) {
        // when the angle is bigger than zero it will mean that the joystick (or some movement)
        // was applied to the player
        this.isMoving = playerMovementResult?.angle.orZero() > 0.0

        // keep the playerMovement
        this.playerMovementResult = playerMovementResult
    }

    fun move(angle: Double, strength: Double) {
        val normalizedPosition = calculateNormalizedPosition(angle, strength)

        val x = normalizedPosition.x * VELOCITY
        val y = normalizedPosition.y * VELOCITY

        this.position.sum(x, y)
    }

    fun moveUsingKeptPlayerMovement() {
        playerMovementResult?.also { playerMovement ->
            val normalizedPosition = calculateNormalizedPosition(
                playerMovement.angle.orZero(),
                playerMovement.strength.orZero()
            )

            val x = normalizedPosition.x * playerMovement.velocity
            val y = normalizedPosition.y * playerMovement.velocity

            this.position.sum(x, y)
        }
    }

    private fun calculateNormalizedPosition(angle: Double, strength: Double): Position {
        return Position(
            cos(angle * Math.PI / 180f) * strength * MatchActivity.SCREEN_DENSITY,
            -sin(angle * Math.PI / 180f) * strength * MatchActivity.SCREEN_DENSITY, // Is negative to invert the direction
        ).normalize()
    }

    fun setPosition(playerMovementResult: PlayerMovementResult?) {
        playerMovementResult?.newPosition?.also {
            position.set(it.x, it.y)
        }
    }

    companion object {
        const val VELOCITY = 8.0
    }
}
