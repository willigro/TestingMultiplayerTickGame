package com.rittmann.myapplication.main.scene

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import android.view.MotionEvent
import com.rittmann.myapplication.main.components.Joystick
import com.rittmann.myapplication.main.core.GameMainThread.Companion.CURRENT_DELTA_TIME
import com.rittmann.myapplication.main.entity.BULLET_DEFAULT_MAX_DISTANCE
import com.rittmann.myapplication.main.entity.BULLET_DEFAULT_VELOCITY
import com.rittmann.myapplication.main.entity.Bullet
import com.rittmann.myapplication.main.entity.Player
import com.rittmann.myapplication.main.entity.Position
import com.rittmann.myapplication.main.entity.server.InputsState
import com.rittmann.myapplication.main.entity.server.PlayerServer
import com.rittmann.myapplication.main.entity.server.WorldState
import com.rittmann.myapplication.main.match.MatchEvents
import com.rittmann.myapplication.main.match.screen.GLOBAL_TAG
import com.rittmann.myapplication.main.match.screen.MatchActivity.Companion.SCREEN_WIDTH
import com.rittmann.myapplication.main.utils.INVALID_ID
import com.rittmann.myapplication.main.utils.Logger

// TODO: move it
const val MIN_STRENGTH_TO_SHOT = 80.0

class SceneMain(
    private val matchEvents: MatchEvents,
) : Scene, Logger {
    private var player: Player? = null
    private var enemies: ArrayList<Player> = arrayListOf()
    private var _bulletsInGame: ArrayList<Bullet> = arrayListOf()
    private var _bulletsToSend: ArrayList<Bullet> = arrayListOf()

    private var joystickMovement: Joystick = Joystick()
    private var joystickAim: Joystick = Joystick()

    val paintText = Paint()

    init {
        paintText.color = Color.WHITE
        paintText.textAlign = Paint.Align.CENTER
        paintText.textSize = 50f

    }

    override fun update(deltaTime: Double, tick: Int) {}

    override fun draw(canvas: Canvas) {
        player?.draw(canvas)
        enemies.forEach { it.draw(canvas) }
        _bulletsInGame.forEach { it.draw(canvas) }


        val xPos = SCREEN_WIDTH / 2
        var yPos = 40

        canvas.drawText("delta=$CURRENT_DELTA_TIME", xPos.toFloat(), yPos.toFloat(), paintText)

        yPos += 40

        canvas.drawText(
            "tick=${SceneManager.clientTickNumber}",
            xPos.toFloat(),
            yPos.toFloat(),
            paintText
        )

        yPos += 40

        canvas.drawText(
            "Last tick received=${SceneManager.clientLastReceivedStateTick}",
            xPos.toFloat(),
            yPos.toFloat(),
            paintText
        )

        yPos += 40

        canvas.drawText(
            "Size received=${SceneManager.clientStateMessagesSize}",
            xPos.toFloat(),
            yPos.toFloat(),
            paintText
        )

        yPos += 40

        canvas.drawText(
            "Size sent=${SceneManager.serverInputMessagesSize}",
            xPos.toFloat(),
            yPos.toFloat(),
            paintText
        )
    }

    override fun finishFrame() {
//        _bulletsToSend.clear()
    }

    override fun receiveTouch(motionEvent: MotionEvent) {

    }

    override fun ownPlayerCreated(player: Player) {
        "ownPlayerCreated=${player.playerId}".log()
        player.retrieveCollider().enable()
        this.player = player
    }

    override fun newPlayerConnected(player: Player) {
        "newPlayerConnected=${player.playerId}".log()
        player.retrieveCollider().enable()
        enemies.add(player)
    }

    override fun playerDisconnected(id: String) {
        val index = enemies.indexOfFirst { it.playerId == id }

        if (index != INVALID_ID) {
            enemies.removeAt(index)
        }
    }

    override fun getPlayer(): Player? {
        return player
    }

    override fun onJoystickMovementChanged(angle: Double, strength: Double) {
        joystickMovement.set(angle, strength)
    }

    override fun onJoystickAimChanged(angle: Double, strength: Double) {
        joystickAim.set(angle, strength)
    }

    // It will force a state, used to FORWARD
    // TODO: simplify it cause it will force everything without check because the forward is a hard replace
    override fun onWorldUpdated(worldState: WorldState, deltaTime: Double) {
        worldState.playerUpdate.players.forEach { playerServer ->
            if (playerServer.id == player?.playerId) {
                player?.also { player ->
                    updatePlayerPosition(playerServer, player, deltaTime)
                }
            } else {
                val enemy = enemies.firstOrNull { it.playerId == playerServer.id }
                enemy?.also { player ->
                    updatePlayerPosition(playerServer, player, deltaTime)
                }
            }
        }
        "".log()

        _bulletsInGame.forEach {
            "At tick=${worldState.tick}, IN GAME position=${it.position}".log()
        }

        worldState.bulletUpdate?.bulletsToRemove?.forEach { bulletToRemove ->
            val index = _bulletsInGame.indexOfFirst { it.bulletId == bulletToRemove.bulletId }
            if (index != INVALID_ID) {
                "At tick=${worldState.tick}, REMOVE the bullet at position=${_bulletsInGame[index].position}".log()
                _bulletsInGame.removeAt(index)
            }
        }
        worldState.bulletUpdate?.bullets?.forEach { bullet ->
            "At tick=${worldState.tick}, REMOTE position=${bullet.position}".log()
            // The local bullets are only representative, I don't need to handle it properly for a while
            // TODO: I guess that if cause a small error the bullet don't collide with the right body
            //  (colliding on server but does not colliding locale) I will need to check if the bullet is still alive
            //  for that I'll need the bullet ID
//                if (bullet.ownerId == player?.playerId) {
//                    continue
//                }

//                "processing the bullet=$bullet".log()

            var found = false
            for (i in 0 until _bulletsInGame.size) {
                if (_bulletsInGame[i].bulletId == bullet.bulletId) {
                    "At tick=${worldState.tick}, replacing the bullet position=${bullet.position}".log()
                    found = true
                    _bulletsInGame[i] = Bullet(
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
                    break
                }
            }

            // create a new one
            if (found.not()) {
                "At tick=${worldState.tick}, adding the bullet position=${bullet.position}".log()
                _bulletsInGame.add(
                    Bullet(
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
                )
            }
        }

        _bulletsInGame.forEach {
            ("Tick " + SceneManager.tickNumberBeenProcessed +
                    " From=" + SceneManager.processState +
                    " Bullet=" + it.bulletId +
                    " New X=" + it.position.x +
                    " New Y=" + it.position.y + " bulletMoving").log()
        }
    }

    // It will force a state when there is an error, used to REPLAY and REWIND
    override fun onWorldUpdated(
        worldState: WorldState,
        errors: List<SceneManager.Error>,
        deltaTime: Double
    ) {
        val allPlayers = arrayListOf<Player>()
        allPlayers.addAll(enemies)
        player?.let { allPlayers.add(it) }

        // TODO: check one a one later seeing if it was a ANGLE or POSITION
        worldState.playerUpdate.players.forEach { playerServer ->

            if (errors.firstOrNull { it.id == playerServer.id } != null) {

                val player = allPlayers.firstOrNull { it.playerId == playerServer.id }

                player?.also {
                    updatePlayerPosition(playerServer, player, deltaTime)
                }
            }
        }

        "".log()

        _bulletsInGame.forEach {
            "At tick=${worldState.tick}, IN GAME position=${it.position}".log()
        }

        worldState.bulletUpdate?.bulletsToRemove?.forEach { bulletToRemove ->
            val index = _bulletsInGame.indexOfFirst { it.bulletId == bulletToRemove.bulletId }
            if (index != INVALID_ID) {
                "At tick=${worldState.tick}, REMOVE the bullet at position=${_bulletsInGame[index].position}".log()
                _bulletsInGame.removeAt(index)
            }
        }

        if (errors.firstOrNull { it is SceneManager.Error.BulletPositionError || it is SceneManager.Error.BulletNotFoundError } != null) {
            worldState.bulletUpdate?.bullets?.forEach { remoteBullet ->
                "At tick=${worldState.tick}, REMOTE position=${remoteBullet.position}".log()

                var found = false
                for (i in 0 until _bulletsInGame.size) {
                    if (_bulletsInGame[i].bulletId == remoteBullet.bulletId) {
                        "At tick=${worldState.tick}, replacing the bullet position=${remoteBullet.position}".log()
                        found = true
                        _bulletsInGame[i] = Bullet.build(remoteBullet)
                        break
                    }
                }

                // create a new one
                if (found.not()) {
                    "At tick=${worldState.tick}, adding the bullet position=${remoteBullet.position}".log()
                    _bulletsInGame.add(
                        Bullet.build(remoteBullet)
                    )
                }
            }
        }

        _bulletsInGame.forEach {
            ("Tick " + SceneManager.tickNumberBeenProcessed +
                    " From=" + SceneManager.processState +
                    " Bullet=" + it.bulletId +
                    " New X=" + it.position.x +
                    " New Y=" + it.position.y + " bulletMoving").log()
        }
    }

    // It will run a new state based on the inputs
    override fun onWorldUpdated(inputsState: InputsState, deltaTime: Double, tick: Int) {
        inputsState.apply {

            // Handle the movement and the aiming
            player?.also { player ->
                player.move(
                    deltaTime,
                    playerInputsState.playerMovementInputsState.angle,
                    playerInputsState.playerMovementInputsState.strength,
                )

                player.aim(
                    playerInputsState.playerAimInputsState.angle,
                    playerInputsState.playerAimInputsState.strength,
                )

                if (player.aimStrength > MIN_STRENGTH_TO_SHOT) {
                    player.shoot(tick)?.also { bullet ->
                        createNewBullet(bullet)
                    }
                }
            }

            // Update the bullet state
            updateBullets(deltaTime)
        }
    }

    override fun getEnemies(): ArrayList<Player> {
        return enemies
    }

    override fun getBulletsToSend(tick: Int): List<Bullet> {
        return _bulletsToSend
    }

    private fun updatePlayerPosition(
        playerServer: PlayerServer,
        player: Player,
        deltaTime: Double
    ) {
        player.move(
            deltaTime,
            playerServer.playerMovement.angle,
            playerServer.playerMovement.strength,
            playerServer.playerMovement.position,
        )

        player.aim(
            playerServer.playerAim.angle,
            playerServer.playerAim.strength,
        )
    }

    private fun createNewBullet(bullet: Bullet) {
        _bulletsInGame.add(bullet)
        _bulletsToSend.add(bullet.copy())
    }

    private fun updateBullets(deltaTime: Double) {
        val bulletIterator = _bulletsInGame.iterator()
        while (bulletIterator.hasNext()) {
            val currentBullet = bulletIterator.next()

            if (currentBullet.isFree(deltaTime)) {
                currentBullet.free()
                bulletIterator.remove()

                val index = _bulletsToSend.indexOfFirst { it.bulletId == currentBullet.bulletId }
                _bulletsToSend.removeAt(index)
            } else {
                currentBullet.update(deltaTime)
            }
        }
    }

    init {
        Log.i(GLOBAL_TAG, "Cena principal criada")
    }
}