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
import com.rittmann.myapplication.main.entity.collisor.GlobalCollisions
import com.rittmann.myapplication.main.entity.server.WorldState
import com.rittmann.myapplication.main.match.MatchEvents
import com.rittmann.myapplication.main.match.screen.GLOBAL_TAG
import com.rittmann.myapplication.main.utils.INVALID_ID
import com.rittmann.myapplication.main.utils.Logger


class SceneMain(
    private val matchEvents: MatchEvents,
) : Scene, Logger {
    private var player: Player? = null
    private var enemies: ArrayList<Player> = arrayListOf()
    private var _bulletTest: ArrayList<Bullet> = arrayListOf()

    private var joystickMovement: Joystick = Joystick()
    private var joystickAim: Joystick = Joystick()

    override fun update(deltaTime: Double, tick: Int) {
//        deltaTime.toString().log()
        player?.also { player ->
            if (joystickMovement.isWorking) {
                player.move(
                    deltaTime,
                    joystickMovement.angle,
                    joystickMovement.strength,
                )
            }

            if (joystickAim.isWorking) {
                player.aim(
                    joystickAim.angle,
                )

                // create a const
                if (joystickAim.strength > 80f) {
                    player.shot()?.also { bullet ->
                        _bulletTest.add(bullet)
                        matchEvents.shoot(bullet)
                    }
                }
            }

            player.update(deltaTime)

            enemies.forEach {
                it.update(deltaTime)
            }

            GlobalCollisions.verifyCollisions()
        }
        updateBullets(deltaTime)
    }

    override fun draw(canvas: Canvas) {
        player?.draw(canvas)
        enemies.forEach { it.draw(canvas) }
        _bulletTest.forEach { it.draw(canvas) }
    }

    override fun terminate() {}

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

    // Improve it later
    override fun onPlayerUpdate(worldState: WorldState, deltaTime: Double, force: Boolean) {
        worldState.playerUpdate.players.forEach { playerServer ->
            if (playerServer.id == player?.playerId) {
                if (force) {
                    player?.move(
                        playerServer.playerMovement.angle,
                        playerServer.playerMovement.strength,
                        playerServer.playerMovement.position,
                    )
                } else {
                    player?.move(
                        deltaTime,
                        playerServer.playerMovement.angle,
                        playerServer.playerMovement.strength,
                    )
                }

                // TODO: make the reconciliation
                player?.aim(
                    playerServer.playerAim.angle,
                )
            } else {
                // I'm going to use the above move here
                val enemy = enemies.firstOrNull { it.playerId == playerServer.id }

                enemy?.keepTheNextPlayerMovement(playerServer)
            }
        }

        worldState.bulletUpdate?.bullets?.forEach { bullet ->
            var localBullet: Bullet? = null
            for (i in 0 until _bulletTest.size) {
                if (_bulletTest[i].bulletId == bullet.bulletId) {
                    localBullet = _bulletTest[i]
                    break
                }
            }

            if (localBullet == null) {
                // create a new one
                _bulletTest.add(
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
            } else {
                // update
                localBullet.updateValues(bullet)
            }
        }
    }

    override fun getEnemies(): List<Player> {
        return enemies
    }

    private fun updateBullets(deltaTime: Double) {
        val bulletIterator = _bulletTest.iterator()

        while (bulletIterator.hasNext()) {
            val currentBullet = bulletIterator.next()

            if (currentBullet.isFree()) {
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