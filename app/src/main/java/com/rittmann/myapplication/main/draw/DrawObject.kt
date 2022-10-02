package com.rittmann.myapplication.main.draw

import android.graphics.Canvas

interface DrawObject {
    fun update()
    fun draw(canvas: Canvas)
    fun free()
}