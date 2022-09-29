package com.rittmann.myapplication.main.scene

import android.graphics.Canvas
import android.view.MotionEvent
import com.rittmann.myapplication.main.entity.Player
import com.rittmann.myapplication.main.entity.Position
import com.rittmann.myapplication.main.entity.server.PlayerMovementResult

interface Scene {
    fun update()
    fun draw(canvas: Canvas)
    fun terminate()
    fun receiveTouch(motionEvent: MotionEvent)
    fun ownPlayerCreated(player: Player)
    fun newPlayerConnected(player: Player)
    fun setJoystickLeftValues(angle: Double, strength: Double)
    fun getPlayerPosition(): Position
    fun playerMovement(playerMovementResult: PlayerMovementResult)
}