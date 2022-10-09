package com.rittmann.myapplication.main.scene

import android.graphics.Canvas
import android.util.Log
import android.view.MotionEvent
import com.rittmann.myapplication.main.components.Joystick
import com.rittmann.myapplication.main.entity.BULLET_DEFAULT_MAX_DISTANCE
import com.rittmann.myapplication.main.entity.BULLET_DEFAULT_VELOCITY
import com.rittmann.myapplication.main.entity.Bullet
import com.rittmann.myapplication.main.entity.Player
import com.rittmann.myapplication.main.entity.Position
import com.rittmann.myapplication.main.entity.server.InputsState
import com.rittmann.myapplication.main.entity.server.WorldState
import com.rittmann.myapplication.main.match.MatchEvents
import com.rittmann.myapplication.main.match.screen.GLOBAL_TAG
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

    override fun update(deltaTime: Double, tick: Int) {}

    override fun draw(canvas: Canvas) {
        player?.draw(canvas)
        enemies.forEach { it.draw(canvas) }
        _bulletsInGame.forEach { it.draw(canvas) }
    }

    override fun finishFrame() {
//        "finishFrame=${_bulletsToSend.size}".log()
        _bulletsToSend.clear()
    }

    override fun receiveTouch(motionEvent: MotionEvent) {

    }

    override fun ownPlayerCreated(player: Player) {
        "ownPlayerCreated".log()
        player.retrieveCollider().enable()
        this.player = player
    }

    override fun newPlayerConnected(player: Player) {
        "newPlayerConnected".log()
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

    // It will force a state, used to replay a stat
    override fun onWorldUpdated(worldState: WorldState, deltaTime: Double) {
        worldState.playerUpdate.players.forEach { playerServer ->
            if (playerServer.id == player?.playerId) {
                player?.also { player ->
                    player.move(
                        deltaTime,
                        playerServer.playerMovement.angle,
                        playerServer.playerMovement.strength,
                        playerServer.playerMovement.position,
                    )

                    player.aim(
                        playerServer.playerAim.angle,
                    )

                    // TODO: as the bullets are been brought from the server, I don't guess that I'll need to create
                    //  a new bullet using this approach, it can duplicate a bullet, or create a wrong bullet that
                    //  will not been sent
//                    if (playerServer.playerAim.strength > MIN_STRENGTH_TO_SHOT) {
//                        player.shoot()?.also { bullet ->
//                            "creating a bullet".log()
//                            createNewBullet(bullet)
//                        }
//                    }
                }
            } else {
                // I'm going to use the above move here
                val enemy = enemies.firstOrNull { it.playerId == playerServer.id }

                enemy?.also { player ->
                    player.move(
                        deltaTime,
                        playerServer.playerMovement.angle,
                        playerServer.playerMovement.strength,
                        playerServer.playerMovement.position,
                    )

                    player.aim(
                        playerServer.playerAim.angle,
                    )

//                    if (playerServer.playerAim.strength > MIN_STRENGTH_TO_SHOT) {
//                        player.shoot()?.also { bullet ->
//                            "creating a bullet".log()
//                            createNewBullet(bullet)
//                        }
//                    }
                }
            }
        }

        worldState.bulletUpdate?.bullets?.also { bullets ->
            for (bullet in bullets) {
                // The local bullets are only representative, I don't need to handle it properly for a while
                // TODO: I guess that if cause a small error the bullet don't collide with the right body
                //  (colliding on server but does not colliding locale) I will need to check if the bullet is still alive
                //  for that I'll need the bullet ID
                if (bullet.ownerId == player?.playerId) {
                    continue
                }

                "processing the bullet=$bullet".log()

                var found = false
                for (i in 0 until _bulletsInGame.size) {
                    if (_bulletsInGame[i].bulletId == bullet.bulletId) {
                        found = true
                        break
                    }
                }

                // create a new one
                if (found.not()) {
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
                        )
                    )
                }
            }
        }
    }

    // It will run a new state based on the inputs
    override fun onWorldUpdated(inputsState: InputsState, deltaTime: Double) = with(inputsState) {
        playerInputsState.playerAimInputsState

        player?.also { player ->
            player.move(
                deltaTime,
                playerInputsState.playerMovementInputsState.angle,
                playerInputsState.playerMovementInputsState.strength,
            )

            player.aim(
                playerInputsState.playerAimInputsState.angle,
            )

            if (playerInputsState.playerAimInputsState.strength > MIN_STRENGTH_TO_SHOT) {
                player.shoot()?.also { bullet ->
                    createNewBullet(bullet)
                }
            }
        }

        updateBullets(deltaTime)
    }

    override fun getEnemies(): List<Player> {
        return enemies
    }

    override fun getBulletsToSend(tick: Int): List<Bullet> {
        return _bulletsToSend
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
            } else {
                currentBullet.update(deltaTime)
            }
        }
    }

    init {
        Log.i(GLOBAL_TAG, "Cena principal criada")
    }
}