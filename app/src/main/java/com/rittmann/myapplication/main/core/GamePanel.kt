package com.rittmann.myapplication.main.core

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.rittmann.myapplication.main.draw.DrawObject
import com.rittmann.myapplication.main.entity.Player
import com.rittmann.myapplication.main.entity.Position
import com.rittmann.myapplication.main.entity.server.PlayerMovementResult
import com.rittmann.myapplication.main.scene.SceneManager

class GamePanel(context: Context?) : SurfaceView(context),
    DrawObject, SurfaceHolder.Callback {

    private var gameMainThread: GameMainThread? = null
    private val sceneManager: SceneManager

    override fun update() {
        sceneManager.update()
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        sceneManager.draw(canvas)
    }

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

    fun setJoystickLeftValues(angle: Double, strength: Double) {
        sceneManager.setJoystickLeftValues(angle, strength)
    }

    fun getPlayerPosition(): Position {
        return sceneManager.getPlayerPosition()
    }

    fun playerMovement(playerMovementResult: PlayerMovementResult) {
        sceneManager.playerMovement(playerMovementResult)
    }

    init {
        holder.addCallback(this)
        sceneManager = SceneManager()
        isFocusable = true
    }
}