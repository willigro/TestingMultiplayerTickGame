package com.rittmann.myapplication.main.entity

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.rittmann.myapplication.main.draw.DrawObject
import com.rittmann.myapplication.main.entity.body.Body
import com.rittmann.myapplication.main.entity.collisor.Collidable
import com.rittmann.myapplication.main.entity.collisor.Collider
import com.rittmann.myapplication.main.extensions.orZero
import com.rittmann.myapplication.main.entity.server.PlayerAim
import com.rittmann.myapplication.main.entity.server.PlayerMovement
import com.rittmann.myapplication.main.entity.server.PlayerServer
import com.rittmann.myapplication.main.entity.server.wasAimApplied
import com.rittmann.myapplication.main.entity.server.wasPositionMovementApplied
import com.rittmann.myapplication.main.utils.Logger

const val BODY_WIDTH = 40
const val BODY_HEIGHT = 40

data class Player(
    val playerId: String = "",
    val position: Position = Position(),
    val color: String = "",
) : DrawObject, Collidable, Logger {

    private val body: Body = Body(position.copy(), BODY_WIDTH, BODY_HEIGHT)
    private val mainGunPointer: Pointer = Pointer(
        position.copy(),
        BODY_WIDTH.toDouble(),
        BODY_HEIGHT.toDouble(),
    )

    private val collider: Collider = Collider(position.copy(), BODY_WIDTH, BODY_HEIGHT, this)

    /*
    * Player is moving
    * */
    private var isMoving: Boolean = false

    /*
    * Player is aiming (moving the aim joystick)
    * */
    private var isAiming: Boolean = false

    /*
    * Keep the player movement and aim data got from the server
    * */
    private var playerServer: PlayerServer? = null

    private val paint = Paint()

    init {
        paint.color = Color.parseColor(color.ifEmpty { "#FFFFFF" })
    }

    override fun update() {
        updatePlayerMovement()
        updatePlayerAim()
        updateBodyPosition()
        updateColliderPosition()
    }

    private fun updateColliderPosition() {
        collider.move(position)
    }

    private fun updateBodyPosition() {
        body.move(position)
    }

    private fun updatePlayerAim() {
        if (isAiming) {
            if (playerServer.wasAimApplied()) {
                // force position
                aim(playerServer?.playerAim)
            } else {
                // keep moving util a new position is received
                aimUsingKeptPlayerMovement()
            }
        }
    }

    private fun updatePlayerMovement() {
        if (isMoving) {
            // New position received but it was not updated yet
            if (playerServer.wasPositionMovementApplied()) {
                // force position
                setPosition(playerServer?.playerMovement)
            } else {
                // keep moving util a new position is received
                moveUsingKeptPlayerMovement()
            }
        }
    }

    override fun draw(canvas: Canvas) {
        // rotate and in its own axis
        canvas.save()
        canvas.rotate(
            -body.rotationAngle.toFloat(),
            (position.x).toFloat(),
            (position.y).toFloat()
        )
        canvas.drawRect(body.rect, paint)
        canvas.restore()

        mainGunPointer.draw(canvas)
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
                "Player $playerId is Touching a bullet".log()
            }
            is Player -> {
                "Player $playerId is Touching a player".log()
            }
        }
    }

    fun keepTheNextPlayerMovement(playerServer: PlayerServer) {
        // when the angle is bigger than zero it will mean that the joystick (or some movement)
        // was applied to the player
        this.isMoving = playerServer.playerMovement.angle > 0.0
        this.isAiming = playerServer.playerAim.angle > 0.0

        // keep the playerMovement
        this.playerServer = playerServer
    }

    fun move(angle: Double, strength: Double) {
        playerServer?.playerMovement?.resetPositionWasApplied()

        val normalizedPosition = Position.calculateNormalizedPosition(angle, strength)

        val x = normalizedPosition.x * VELOCITY
        val y = normalizedPosition.y * VELOCITY

        this.position.sum(x, y)

        mainGunPointer.moveAndRotate(x, y)
    }

    private fun moveUsingKeptPlayerMovement() {
        playerServer?.playerMovement?.also { playerMovement ->
            val normalizedPosition = Position.calculateNormalizedPosition(
                playerMovement.angle.orZero(),
                playerMovement.strength.orZero()
            )

            val x = normalizedPosition.x * playerMovement.velocity
            val y = normalizedPosition.y * playerMovement.velocity

            this.position.sum(x, y)

            mainGunPointer.setMoveAndRotate(x, y)
        }
    }

    private fun setPosition(playerMovement: PlayerMovement?) {
        playerMovement?.position?.also {
            position.set(it.x, it.y)

            mainGunPointer.setMoveAndRotate(it.x, it.y)
        }
    }

    private fun aimUsingKeptPlayerMovement() {
        playerServer?.playerAim?.also { playerAim ->
            aim(playerAim.angle)
        }
    }

    fun aim(angle: Double) {
        body.setRotation(angle)

        mainGunPointer.setRotation(angle)
    }

    private fun aim(playerAim: PlayerAim?) {
        playerAim?.angle?.also {
            aim(it)
        }
    }

    private var lastTime = 0L
    fun shot(): Bullet? {
        val currentTime = System.currentTimeMillis()

        if (lastTime > 0L && currentTime - lastTime < 500) return null
        lastTime = System.currentTimeMillis()

        return createBullet()
    }

    private fun createBullet() : Bullet {
        val pointerPosition = mainGunPointer.getRotatedPosition()
        val bullet = Bullet(
            bulletId = "${playerId}_${System.nanoTime()}",
            ownerId = playerId,
            position = Position(
                x = pointerPosition.x,
                y = pointerPosition.y
            ),
            angle = body.rotationAngle,
            velocity = BULLET_DEFAULT_VELOCITY,
            maxDistance = BULLET_DEFAULT_MAX_DISTANCE,
        )

        bullet.retrieveCollider().enable()
        return bullet
    }

    companion object {
        const val VELOCITY = 8.0
    }
}
