package com.rittmann.myapplication.main.scene

import android.graphics.Canvas
import android.view.MotionEvent
import com.rittmann.myapplication.main.entity.Player
import com.rittmann.myapplication.main.entity.server.WorldState
import com.rittmann.myapplication.main.match.MatchEvents

class SceneManager(
    private val matchEvents: MatchEvents,
) {
    private val scene: Scene

    init {
        scene = SceneMain(matchEvents)
    }

    fun receiveTouch(motionEvent: MotionEvent) {
        scene.receiveTouch(motionEvent)
    }

    fun update() {
        scene.update()
        matchEvents.update()
    }

    fun draw(canvas: Canvas) {
        scene.draw(canvas)
        matchEvents.draw()
    }

    fun ownPlayerCreated(player: Player) {
        scene.ownPlayerCreated(player)
    }

    fun newPlayerConnected(player: Player) {
        scene.newPlayerConnected(player)
    }

    fun playerDisconnected(id: String) {
        scene.playerDisconnected(id)
    }

    fun getPlayer(): Player? = scene.getPlayer()

    fun onJoystickMovementChanged(angle: Double, strength: Double) {
        scene.onJoystickMovementChanged(angle, strength)
    }

    fun onJoystickAimChanged(angle: Double, strength: Double) {
        scene.onJoystickAimChanged(angle, strength)
    }

    fun onPlayerUpdate(worldState: WorldState) {
        scene.onPlayerUpdate(worldState)
    }

    fun getEnemies(): List<Player> {
        return scene.getEnemies()
    }
}