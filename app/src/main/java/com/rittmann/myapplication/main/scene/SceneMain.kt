package com.rittmann.myapplication.main.scene

import android.graphics.Canvas
import android.util.Log
import android.view.MotionEvent
import com.rittmann.myapplication.main.components.Joystick
import com.rittmann.myapplication.main.entity.Player
import com.rittmann.myapplication.main.entity.Position
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
                    cos(joystickLeft.angle * Math.PI / 180f) * joystickLeft.strength * MatchActivity.SCREEN_DENSITY,
                    -sin(joystickLeft.angle * Math.PI / 180f) * joystickLeft.strength * MatchActivity.SCREEN_DENSITY, // Is negative to invert the direction
                ).normalize().apply {
                    Log.i(GLOBAL_TAG, "vector=$this")
                }
            )
        }
        player?.update()
        enemies.forEach { it.update() }
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

    init {
        Log.i(GLOBAL_TAG, "Cena principal criada")
    }
}