package com.rittmann.myapplication.main.scene

import android.graphics.Canvas
import android.view.MotionEvent
import com.rittmann.myapplication.main.entity.Bullet
import com.rittmann.myapplication.main.entity.Player
import com.rittmann.myapplication.main.entity.server.InputsState
import com.rittmann.myapplication.main.entity.server.WorldState

// TODO organize by server and interface
interface Scene {
    fun update(deltaTime: Double, tick: Int)
    fun draw(canvas: Canvas)
    fun finishFrame()
    fun receiveTouch(motionEvent: MotionEvent)
    fun ownPlayerCreated(player: Player)
    fun newPlayerConnected(player: Player)
    fun playerDisconnected(id: String)
    fun getPlayer(): Player?
    fun onJoystickMovementChanged(angle: Double, strength: Double)
    fun onJoystickAimChanged(angle: Double, strength: Double)
    fun onWorldUpdated(worldState: WorldState, deltaTime: Double)
    fun onWorldUpdated(worldState: WorldState, errors: List<SceneManager.Error>, deltaTime: Double)
    fun onWorldUpdated(inputsState: InputsState, deltaTime: Double, tick: Int)
    fun getEnemies(): ArrayList<Player>
    fun getBulletsToSend(tick: Int): List<Bullet>
}