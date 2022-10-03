package com.rittmann.myapplication.main.scene

import android.graphics.Canvas
import android.view.MotionEvent
import com.rittmann.myapplication.main.entity.Player
import com.rittmann.myapplication.main.entity.Position
import com.rittmann.myapplication.main.entity.server.WorldState

// TODO organize by server and interface
interface Scene {
    fun update()
    fun draw(canvas: Canvas)
    fun terminate()
    fun receiveTouch(motionEvent: MotionEvent)
    fun ownPlayerCreated(player: Player)
    fun newPlayerConnected(player: Player)
    fun playerDisconnected(id: String)
    fun onJoystickMovementChanged(angle: Double, strength: Double)
    fun onJoystickAimChanged(angle: Double, strength: Double)
    fun getPlayerPosition(): Position
    fun onPlayerUpdate(worldState: WorldState)
}