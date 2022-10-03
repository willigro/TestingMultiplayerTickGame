package com.rittmann.myapplication.main.scene

import android.graphics.Canvas
import android.view.MotionEvent
import com.rittmann.myapplication.main.entity.Player
import com.rittmann.myapplication.main.entity.Position
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
        matchEvents.update()
        scene.update()
    }

    fun draw(canvas: Canvas) {
        scene.draw(canvas)
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

    fun onJoystickMovementChanged(angle: Double, strength: Double) {
        scene.onJoystickMovementChanged(angle, strength)
    }

    fun onJoystickAimChanged(angle: Double, strength: Double) {
        scene.onJoystickAimChanged(angle, strength)
    }

    fun getPlayerPosition(): Position {
        return scene.getPlayerPosition()
    }

    fun onPlayerUpdate(worldState: WorldState) {
        scene.onPlayerUpdate(worldState)
    }
}