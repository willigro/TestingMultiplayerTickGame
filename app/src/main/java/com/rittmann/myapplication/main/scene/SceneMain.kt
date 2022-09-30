package com.rittmann.myapplication.main.scene

import android.graphics.Canvas
import android.util.Log
import android.view.MotionEvent
import com.rittmann.myapplication.main.components.Joystick
import com.rittmann.myapplication.main.entity.Player
import com.rittmann.myapplication.main.entity.Position
import com.rittmann.myapplication.main.entity.server.PlayerMovementWrapResult
import com.rittmann.myapplication.main.match.screen.GLOBAL_TAG

class SceneMain : Scene {
    private var player: Player? = null
    private var enemies: ArrayList<Player> = arrayListOf()

    private var joystickMovement: Joystick = Joystick()
    private var joystickAim: Joystick = Joystick()

    override fun update() {
        if (joystickMovement.isWorking) {
            player?.move(
                joystickMovement.angle,
                joystickMovement.strength,
            )
        }

        if (joystickAim.isWorking) {
            player?.aim(
                joystickAim.angle,
            )
        }

        player?.update()

        enemies.forEach {
            it.update()
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
        this.player = player
    }

    override fun newPlayerConnected(player: Player) {
        enemies.add(player)
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

    init {
        Log.i(GLOBAL_TAG, "Cena principal criada")
    }
}