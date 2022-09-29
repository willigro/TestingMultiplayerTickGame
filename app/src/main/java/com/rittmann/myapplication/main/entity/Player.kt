package com.rittmann.myapplication.main.entity

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import com.rittmann.myapplication.main.entity.body.Collider
import com.rittmann.myapplication.main.entity.body.Body
import com.rittmann.myapplication.main.draw.DrawObject
import com.rittmann.myapplication.main.match.screen.GLOBAL_TAG

const val BODY_WIDTH = 40
const val BODY_HEIGTH = 40

data class Player(
    val playerId: String = "",
    val position: Position = Position(),
    val color: String = "",
) : DrawObject {

    private val body: Body = Body(BODY_WIDTH, BODY_HEIGTH)
    val collider: Collider = Collider(body)

    private val paint = Paint()

    init {
        paint.color = Color.WHITE// Color.parseColor("#FFFFFF")
    }

    override fun update() {
        body.move(position)
    }

    override fun draw(canvas: Canvas) {
        //Log.i(GLOBAL_TAG, "set - position=${position}, body.rect=${body.rect.toShortString()} color=${paint.color}")

        canvas.drawRect(body.rect, paint)
//        canvas.drawRect(100f, 100f, 200f, 200f, paint)
    }

    fun move(position: Position) {
        val x = position.x * VELOCITY
        val y = position.y * VELOCITY

        //Log.i(GLOBAL_TAG, "set - PRE position=${this.position}, body.rect=${body.rect.flattenToString()} color=${paint.color}")

        this.position.sum(x, y)

//        Log.i(GLOBAL_TAG, "set - position=${position}, x=$x, y=$y, this.position=${this.position}")
        Log.i(GLOBAL_TAG, "set - POST position=${this.position}, body.rect=${body.rect.flattenToString()} color=${paint.color}")
    }

    companion object {
        private const val VELOCITY = 8
        private const val VELOCITY_DASH = 8
    }
}
