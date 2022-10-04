package com.rittmann.myapplication.main.draw

import android.graphics.Canvas

interface DrawObject {
    fun update(deltaTime: Float)
    fun draw(canvas: Canvas)
    fun free()
}