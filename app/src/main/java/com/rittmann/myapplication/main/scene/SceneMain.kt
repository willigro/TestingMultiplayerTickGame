package com.rittmann.myapplication.main.scene

import android.graphics.Canvas
import android.util.Log
import android.view.MotionEvent
import com.rittmann.myapplication.main.components.Joystick
import com.rittmann.myapplication.main.entity.Player
import com.rittmann.myapplication.main.entity.Position
import com.rittmann.myapplication.main.entity.server.PlayerMovementResult
import com.rittmann.myapplication.main.extensions.orZero
import com.rittmann.myapplication.main.match.screen.GLOBAL_TAG
import com.rittmann.myapplication.main.match.screen.MatchActivity
import kotlin.math.cos
import kotlin.math.sin

class SceneMain : Scene {
    private var player: Player? = null
    private var enemies: ArrayList<Player> = arrayListOf()

    private var joystickLeft: Joystick = Joystick()

    override fun update() {
        if (joystickLeft.isWorking) {
            player?.move(
                Position(
                    cos(joystickLeft.angle * Math.PI / 180f) * joystickLeft.strength * MatchActivity.SCREEN_DENSITY, // * MatchActivity.SCREEN_DENSITY
                    -sin(joystickLeft.angle * Math.PI / 180f) * joystickLeft.strength * MatchActivity.SCREEN_DENSITY, // Is negative to invert the direction
                ).normalize()
            )
        }
        player?.update()

        enemies.forEach {
            if (it.isMoving) {
                // New position received but it was not updated yet
                if (it.playerMovementResult?.newPositionApplied?.compareAndSet(
                        false,
                        true
                    ) == true
                ) {
                    Log.i(GLOBAL_TAG, "Setting a new position ${it.playerMovementResult?.newPosition}")
                    // force position
                    it.setPosition(it.playerMovementResult)
                } else {
                    // keep moving util a new position is received
                    it.move(
                        Position(
                            cos(it.playerMovementResult?.angle.orZero() * Math.PI / 180f) * it.playerMovementResult?.strength.orZero() * MatchActivity.SCREEN_DENSITY,
                            -sin(it.playerMovementResult?.angle.orZero() * Math.PI / 180f) * it.playerMovementResult?.strength.orZero() * MatchActivity.SCREEN_DENSITY, // Is negative to invert the direction
                        ).normalize()
                    )

                    Log.i(GLOBAL_TAG, "Going to a new position ${it.position}")
                }

            }
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

    override fun setJoystickLeftValues(angle: Double, strength: Double) {
        joystickLeft.set(angle, strength)
    }

    override fun getPlayerPosition(): Position {
        return player?.position ?: Position()
    }

    override fun playerMovement(playerMovementResult: PlayerMovementResult) {
        val movedEnemy = enemies.firstOrNull { it.playerId == playerMovementResult.id }

        movedEnemy?.setIsMoving(playerMovementResult)
    }

    init {
        Log.i(GLOBAL_TAG, "Cena principal criada")
    }
}