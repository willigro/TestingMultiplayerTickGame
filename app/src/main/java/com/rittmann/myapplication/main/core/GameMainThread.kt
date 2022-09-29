package com.rittmann.myapplication.main.core

import android.graphics.Canvas
import android.view.SurfaceHolder

class GameMainThread(private val surfaceHolder: SurfaceHolder, private val game: GamePanel) :
    Thread() {
    private var running = false
    override fun run() {
        var startTime: Long
        var frameCount = 0
        var totalTime: Long = 0
        val targetTime = (1000 / MAX_FPS).toLong()
        val million: Long = 1000000

        while (running) {
            startTime = System.nanoTime()
            canvas = null
            draw()
            calculateSleep(targetTime, startTime, million)
            totalTime += System.nanoTime() - startTime
            frameCount++

            if (frameCount == MAX_FPS) {
                val averageFps =
                    (1000 / (totalTime / frameCount.toFloat() / million.toFloat())).toDouble()
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

    private fun draw() {
        try {
            canvas = surfaceHolder.lockCanvas()
            //garente que apenas uma thread por vez execurata este trecho
            synchronized(surfaceHolder) {
                game.update()
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
        private const val MAX_FPS = 30
        private var canvas: Canvas? = null
    }
}