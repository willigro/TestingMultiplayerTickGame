package com.rittmann.myapplication.main.entity

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.rittmann.myapplication.main.draw.DrawObject
import com.rittmann.myapplication.main.entity.body.Body
import com.rittmann.myapplication.main.entity.collisor.Collidable
import com.rittmann.myapplication.main.entity.collisor.Collider
import com.rittmann.myapplication.main.entity.server.PlayerAimResult
import com.rittmann.myapplication.main.entity.server.PlayerMovementResult
import com.rittmann.myapplication.main.entity.server.PlayerMovementWrapResult
import com.rittmann.myapplication.main.entity.server.PlayerShootingResponseWrap
import com.rittmann.myapplication.main.entity.server.wasAimApplied
import com.rittmann.myapplication.main.entity.server.wasPositionApplied
import com.rittmann.myapplication.main.extensions.orZero
import com.rittmann.myapplication.main.server.PlayerMovement
import com.rittmann.myapplication.main.server.PlayerServer
import com.rittmann.myapplication.main.server.wasPositionAppliedExt
import com.rittmann.myapplication.main.utils.Logger

const val BODY_WIDTH = 40
const val BODY_HEIGHT = 40

data class Player(
    val playerId: String = "",
    val position: Position = Position(),
    val color: String = "",
) : DrawObject, Collidable, Logger {

    private var _bulletTest: ArrayList<Bullet> = arrayListOf()
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
    var isMoving: Boolean = false

    /*
    * Player is aiming (moving the aim joystick)
    * */
    var isAiming: Boolean = false

    /*
    * Keep the player movement and aim data got from the server
    * */
    private var playerMovementResult: PlayerMovementWrapResult? = null
    private var playerMovement: PlayerMovement? = null

    private val paint = Paint()

    init {
        paint.color = Color.parseColor(color.ifEmpty { "#FFFFFF" })
    }

    override fun update() {
        updatePlayerMovement()
        updatePlayerAim()
        updateBodyPosition()
        updateColliderPosition()
        updateBullets()
    }

    private fun updateColliderPosition() {
        collider.move(position)
    }

    private fun updateBullets() {
        val bulletIterator = _bulletTest.iterator()

        while (bulletIterator.hasNext()) {
            val currentBullet = bulletIterator.next()

            if (currentBullet.isFree()) {
                currentBullet.free()
                bulletIterator.remove()
            } else {
                currentBullet.update()
            }
        }
    }

    private fun updateBodyPosition() {
        body.move(position)
    }

    private fun updatePlayerAim() {
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
    }

    private fun updatePlayerMovement() {
        if (isMoving) {
//            // New position received but it was not updated yet
//            if (playerMovementResult.wasPositionApplied()) {
//                // force position
//                setPosition(playerMovementResult?.playerMovementResult)
//            } else {
//                // keep moving util a new position is received
//                moveUsingKeptPlayerMovement()
//            }

            // New position received but it was not updated yet
            if (playerMovement.wasPositionAppliedExt()) {
                // force position
                setPosition(playerMovement)
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

        _bulletTest.forEach { it.draw(canvas) }
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

    fun keepTheNextPlayerMovement(playerMovementWrapResult: PlayerMovementWrapResult?) {
        // when the angle is bigger than zero it will mean that the joystick (or some movement)
        // was applied to the player
        this.isMoving = playerMovementWrapResult?.playerMovementResult?.angle.orZero() > 0.0
        this.isAiming = playerMovementWrapResult?.playerAimResult?.angle.orZero() > 0.0

        // keep the playerMovement
        this.playerMovementResult = playerMovementWrapResult
    }

    fun keepTheNextPlayerMovement(playerMovement: PlayerMovement) {
        // when the angle is bigger than zero it will mean that the joystick (or some movement)
        // was applied to the player
        this.isMoving = playerMovement.angle > 0.0

        // keep the playerMovement
        this.playerMovement = playerMovement
    }

    fun move(angle: Double, strength: Double) {
        playerMovement?.resetPositionWasApplied()

        val normalizedPosition = Position.calculateNormalizedPosition(angle, strength)

        val x = normalizedPosition.x * VELOCITY
        val y = normalizedPosition.y * VELOCITY

        this.position.sum(x, y)

        mainGunPointer.moveAndRotate(x, y)
    }

    private fun moveUsingKeptPlayerMovementResult() {
        playerMovementResult?.playerMovementResult?.also { playerMovement ->
            val normalizedPosition = Position.calculateNormalizedPosition(
                playerMovement.angle.orZero(),
                playerMovement.strength.orZero()
            )

            val x = normalizedPosition.x * playerMovement.velocity
            val y = normalizedPosition.y * playerMovement.velocity

            this.position.sum(x, y)

            mainGunPointer.moveAndRotate(x, y)
        }
    }

    private fun moveUsingKeptPlayerMovement() {
        playerMovement?.also { playerMovement ->
            val normalizedPosition = Position.calculateNormalizedPosition(
                playerMovement.angle.orZero(),
                playerMovement.strength.orZero()
            )

            val x = normalizedPosition.x * playerMovement.velocity
            val y = normalizedPosition.y * playerMovement.velocity

            this.position.sum(x, y)

            mainGunPointer.moveAndRotate(x, y)
        }
    }

    private fun setPosition(playerMovementResult: PlayerMovementResult?) {
        playerMovementResult?.newPosition?.also {
            position.set(it.x, it.y)

            mainGunPointer.moveAndRotate(it.x, it.y)
        }
    }

    private fun setPosition(playerMovement: PlayerMovement?) {
        playerMovement?.position?.also {
            position.set(it.x, it.y)

            mainGunPointer.moveAndRotate(it.x, it.y)
        }
    }

    private fun aimUsingKeptPlayerMovement() {
        playerMovementResult?.playerAimResult?.also { playerAim ->
            aim(playerAim.angle)
        }
    }

    fun aim(angle: Double) {
        body.setRotation(angle)

        mainGunPointer.setRotation(angle)
    }

    private fun aim(playerAimResult: PlayerAimResult?) {
        playerAimResult?.angle?.also {
            aim(it)
        }
    }

    var lastTime = 0L
    fun shot(): Bullet? {
        val currentTime = System.currentTimeMillis()

        if (lastTime > 0L && currentTime - lastTime < 500) return null
        lastTime = System.currentTimeMillis()

        val pointerPosition = mainGunPointer.getRotatedPosition()
        val bullet = Bullet(
            bulletId = "",
            position = Position(
                x = pointerPosition.x,
                y = pointerPosition.y
            ),
            angle = body.rotationAngle,
        )

        bullet.retrieveCollider().enable()
        _bulletTest.add(bullet)
        return bullet
    }

    fun shot(shootingResponseWrap: PlayerShootingResponseWrap): Bullet? {
        val currentTime = System.currentTimeMillis()

        if (lastTime > 0L && currentTime - lastTime < 500) return null
        lastTime = System.currentTimeMillis()

        val bullet = shootingResponseWrap.bullet

        bullet.retrieveCollider().enable()
        _bulletTest.add(bullet)
        return bullet
    }

    companion object {
        const val VELOCITY = 8.0
    }
}
