package com.rittmann.myapplication.main.core

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.rittmann.myapplication.main.draw.DrawObject
import com.rittmann.myapplication.main.entity.Player
import com.rittmann.myapplication.main.match.MatchEvents
import com.rittmann.myapplication.main.scene.SceneManager
import com.rittmann.myapplication.main.entity.server.WorldState

class GamePanel(
    context: Context,
) : SurfaceView(context), DrawObject, SurfaceHolder.Callback {

    private var gameMainThread: GameMainThread? = null
    private lateinit var sceneManager: SceneManager

    init {
        holder.addCallback(this)
        isFocusable = true
    }

    fun build(matchEvents: MatchEvents): GamePanel {
        sceneManager = SceneManager(matchEvents)
        return this
    }

    override fun update(deltaTime: Float) {
        sceneManager.update(deltaTime)
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        sceneManager.draw(canvas)
    }

    override fun free() {}

    override fun surfaceCreated(holder: SurfaceHolder) {
        gameMainThread = GameMainThread(getHolder(), this)
        gameMainThread?.setRunning(true)
        gameMainThread?.start()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        try {
            gameMainThread?.setRunning(false)
            gameMainThread?.join()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        sceneManager.receiveTouch(event)
        return true
    }

    fun ownPlayerCreated(player: Player) {
        sceneManager.ownPlayerCreated(player)
    }

    fun newPlayerConnected(player: Player) {
        sceneManager.newPlayerConnected(player)
    }

    fun onJoystickMovementChanged(angle: Double, strength: Double) {
        sceneManager.onJoystickMovementChanged(angle, strength)
    }

    fun onJoystickAimChanged(angle: Double, strength: Double) {
        sceneManager.onJoystickAimChanged(angle, strength)
    }

    fun getPlayer(): Player? {
        return sceneManager.getPlayer()
    }

    fun getEnemies(): List< Player> {
        return sceneManager.getEnemies()
    }

    fun playerDisconnected(id: String) {
        sceneManager.playerDisconnected(id)
    }

    fun onPlayerUpdate(worldState: WorldState) {
        sceneManager.onPlayerUpdate(worldState)
    }
}