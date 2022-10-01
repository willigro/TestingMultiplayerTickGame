package com.rittmann.myapplication.main.entity

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.rittmann.myapplication.main.draw.DrawObject
import com.rittmann.myapplication.main.entity.body.Body
import com.rittmann.myapplication.main.entity.body.Collider
import com.rittmann.myapplication.main.entity.server.PlayerAimResult
import com.rittmann.myapplication.main.entity.server.PlayerMovementResult
import com.rittmann.myapplication.main.entity.server.PlayerMovementWrapResult
import com.rittmann.myapplication.main.entity.server.wasAimApplied
import com.rittmann.myapplication.main.entity.server.wasPositionApplied
import com.rittmann.myapplication.main.extensions.orZero
import com.rittmann.myapplication.main.match.screen.MatchActivity
import com.rittmann.myapplication.main.utils.Logger
import kotlin.math.cos
import kotlin.math.sin

const val BODY_WIDTH = 40
const val BODY_HEIGHT = 40

data class Player(
    val playerId: String = "",
    val position: Position = Position(),
    val color: String = "",
) : DrawObject, Collidable, Logger {

    private val body: Body = Body(BODY_WIDTH, BODY_HEIGHT)
    private val mainGunPointer: Pointer = Pointer()

    private val collider: Collider = Collider(BODY_WIDTH, BODY_HEIGHT)

    /*
    * Player is moving
    * */
    var isMoving: Boolean = false

    /*
    * Player is aiming (moving the aim joystick)
    * */
    var isAiming: Boolean = false

    /*
    * Keep the player movement and aim data got from the server
    * */
    var playerMovementResult: PlayerMovementWrapResult? = null

    private val paint = Paint()

    init {
        paint.color = Color.parseColor(color.ifEmpty { "#FFFFFF" })
    }

    override fun update() {
        if (isMoving) {
            // New position received but it was not updated yet
            if (playerMovementResult.wasPositionApplied()) {
                // force position
                setPosition(playerMovementResult?.playerMovementResult)
            } else {
                // keep moving util a new position is received
                moveUsingKeptPlayerMovement()
            }
        }

        if (isAiming) {
            // New position received but it was not updated yet
            if (playerMovementResult.wasAimApplied()) {
                // force position
                aim(playerMovementResult?.playerAimResult)
            } else {
                // keep moving util a new position is received
                aimUsingKeptPlayerMovement()
            }
        }

        body.move(position)
        mainGunPointer.move(position.x + BODY_WIDTH, position.y + (BODY_HEIGHT / 2))
    }

    override fun draw(canvas: Canvas) {
        canvas.save()
        canvas.rotate(
            -body.rotationAngle.toFloat(),
            (position.x).toFloat(),
            (position.y).toFloat()
        )
        canvas.drawRect(body.rect, paint)

        // it will rotate together to the body
        mainGunPointer.draw(canvas)

        canvas.restore()
    }

    override fun retrieveCollider(): Collider {
        return collider
    }

    override fun collidingWith(collidable: Collidable) {
        when (collidable) {
            is Bullet -> {
                "Player $playerId is Touching a bullet".log()
            }
            is Player -> {
                "Player $playerId is Touching a player".log()
            }
        }
    }

    fun keepTheNextPlayerMovement(playerMovementWrapResult: PlayerMovementWrapResult?) {
        // when the angle is bigger than zero it will mean that the joystick (or some movement)
        // was applied to the player
        this.isMoving = playerMovementWrapResult?.playerMovementResult?.angle.orZero() > 0.0
        this.isAiming = playerMovementWrapResult?.playerAimResult?.angle.orZero() > 0.0

        // keep the playerMovement
        this.playerMovementResult = playerMovementWrapResult
    }

    fun move(angle: Double, strength: Double) {
        val normalizedPosition = calculateNormalizedPosition(angle, strength)

        val x = normalizedPosition.x * VELOCITY
        val y = normalizedPosition.y * VELOCITY

        this.position.sum(x, y)
    }

    private fun moveUsingKeptPlayerMovement() {
        playerMovementResult?.playerMovementResult?.also { playerMovement ->
            val normalizedPosition = calculateNormalizedPosition(
                playerMovement.angle.orZero(),
                playerMovement.strength.orZero()
            )

            val x = normalizedPosition.x * playerMovement.velocity
            val y = normalizedPosition.y * playerMovement.velocity

            this.position.sum(x, y)
        }
    }

    private fun aimUsingKeptPlayerMovement() {
        playerMovementResult?.playerAimResult?.also { playerAim ->
            aim(playerAim.angle)
        }
    }

    private fun calculateNormalizedPosition(angle: Double, strength: Double): Position {
        return Position(
            cos(angle * Math.PI / 180f) * strength * MatchActivity.SCREEN_DENSITY,
            -sin(angle * Math.PI / 180f) * strength * MatchActivity.SCREEN_DENSITY, // Is negative to invert the direction
        ).normalize()
    }

    private fun setPosition(playerMovementResult: PlayerMovementResult?) {
        playerMovementResult?.newPosition?.also {
            position.set(it.x, it.y)
        }
    }

    fun aim(angle: Double) {
        body.setRotation(angle)
    }

    private fun aim(playerAimResult: PlayerAimResult?) {
        playerAimResult?.angle?.also {
            aim(it)
        }
    }

    fun shot() {

    }

    companion object {
        const val VELOCITY = 8.0
    }
}
