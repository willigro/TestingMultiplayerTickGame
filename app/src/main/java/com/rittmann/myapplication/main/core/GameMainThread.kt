package com.rittmann.myapplication.main.core

import android.graphics.Canvas
import android.view.SurfaceHolder
import com.rittmann.myapplication.main.utils.Logger

class GameMainThread(
    private val surfaceHolder: SurfaceHolder, private val game: GamePanel
) : Thread(), Logger {
    private val targetTime = (1000 / MAX_FPS).toLong()
    private val million: Long = 1000000

    private var running = false

    override fun run() {
        var startFrameTime: Long
        var lastTime: Long = System.nanoTime()
        var frameCount = 0
        var totalTime: Long = 0
        var deltaTime: Long

        while (running) {
            startFrameTime = System.nanoTime()

            deltaTime = (startFrameTime - lastTime) / million
            lastTime = startFrameTime

//            Thread.currentThread().name.log()
            canvas = null

            update(deltaTime = 0.05) // deltaTime.coerceAtLeast(1) / 1000.0
            calculateSleep(targetTime, startFrameTime, million)
            totalTime += System.nanoTime() - startFrameTime
            frameCount++

            if (frameCount == MAX_FPS) {
//                val averageFps =
//                    (1000 / (totalTime / frameCount.toFloat() / million.toFloat())).toDouble()
                frameCount = 0
                totalTime = 0
            }
        }
    }

    private fun calculateSleep(targetTime: Long, startTime: Long, million: Long) {
        val timeMillis = (System.nanoTime() - startTime) / million
        val waitTime = targetTime - timeMillis
        try {
            if (waitTime > 0) {
                sleep(waitTime)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun update(deltaTime: Double) {
        try {
            CURRENT_DELTA_TIME = deltaTime
            canvas = surfaceHolder.lockCanvas()
            // garente que apenas uma thread por vez execurata este trecho
            synchronized(surfaceHolder) {
                game.update(deltaTime)
                canvas?.also {
                    game.draw(it)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            if (canvas != null) {
                try {
                    surfaceHolder.unlockCanvasAndPost(canvas)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun setRunning(running: Boolean) {
        this.running = running
    }

    companion object {
        const val MAX_FPS = 20
        var CURRENT_DELTA_TIME = 0.0 // Use it for logs
        private var canvas: Canvas? = null
    }
}