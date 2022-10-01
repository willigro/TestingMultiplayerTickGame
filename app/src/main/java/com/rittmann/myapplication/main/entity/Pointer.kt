package com.rittmann.myapplication.main.entity

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.rittmann.myapplication.main.draw.DrawObject
import com.rittmann.myapplication.main.entity.body.PhysicObject

private const val POINTER_SIZE = 10

class Pointer() : PhysicObject(POINTER_SIZE, POINTER_SIZE), DrawObject {

    private val paint = Paint()

    init {
        paint.color = Color.YELLOW
    }

    override fun update() {

    }

    override fun draw(canvas: Canvas) {
        canvas.drawRect(rect, paint)
    }
}