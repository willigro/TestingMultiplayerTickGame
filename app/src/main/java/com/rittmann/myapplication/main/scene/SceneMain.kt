package com.rittmann.myapplication.main.scene

import android.graphics.Canvas
import android.util.Log
import android.view.MotionEvent
import com.rittmann.myapplication.main.components.Joystick
import com.rittmann.myapplication.main.entity.Player
import com.rittmann.myapplication.main.entity.Position
import com.rittmann.myapplication.main.entity.collisor.GlobalCollisions
import com.rittmann.myapplication.main.entity.server.PlayerMovementResult
import com.rittmann.myapplication.main.entity.server.PlayerMovementWrapResult
import com.rittmann.myapplication.main.entity.server.PlayerShootingResponseWrap
import com.rittmann.myapplication.main.match.MatchEvents
import com.rittmann.myapplication.main.match.screen.GLOBAL_TAG
import com.rittmann.myapplication.main.server.PlayerUpdate
import com.rittmann.myapplication.main.utils.INVALID_ID
import com.rittmann.myapplication.main.utils.Logger


class SceneMain(
    private val matchEvents: MatchEvents,
) : Scene, Logger {
    private var player: Player? = null
    private var enemies: ArrayList<Player> = arrayListOf()

    private var joystickMovement: Joystick = Joystick()
    private var joystickAim: Joystick = Joystick()

    override fun update() {
        player?.also { player ->
            if (joystickMovement.isWorking) {
                player.move(
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
                        matchEvents.shoot(player, bullet)
                    }
                }
            }

            player.update()

            enemies.forEach {
                it.update()
            }

            GlobalCollisions.verifyCollisions()
        }
    }

    override fun draw(canvas: Canvas) {
//        canvas.drawColor(StardardColors.INSTANCE.getBackground());
        player?.draw(canvas)
        enemies.forEach { it.draw(canvas) }
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

    override fun onJoystickMovementChanged(angle: Double, strength: Double) {
        joystickMovement.set(angle, strength)
    }

    override fun onJoystickAimChanged(angle: Double, strength: Double) {
        joystickAim.set(angle, strength)
    }

    override fun getPlayerPosition(): Position {
        return player?.position ?: Position()
    }

    override fun playerMovement(playerMovementWrapResult: PlayerMovementWrapResult) {
        val movedEnemy = enemies.firstOrNull { it.playerId == playerMovementWrapResult.id }

        movedEnemy?.keepTheNextPlayerMovement(playerMovementWrapResult)
    }

    override fun onPlayerEnemyShooting(shootingResponseWrap: PlayerShootingResponseWrap) {
        val enemyShooting = enemies.firstOrNull { it.playerId == shootingResponseWrap.playerId }

        enemyShooting?.shot(shootingResponseWrap)
    }

    override fun onPlayerUpdate(playerUpdate: PlayerUpdate) {
        playerUpdate.players.forEach { playerServer ->
            if (playerServer.id == player?.playerId) {
                player?.keepTheNextPlayerMovement(playerServer)
            } else {
                val enemy = enemies.firstOrNull { it.playerId == playerServer.id }

                enemy?.keepTheNextPlayerMovement(playerServer)
            }
        }
    }

    init {
        Log.i(GLOBAL_TAG, "Cena principal criada")
    }
}