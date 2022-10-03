package com.rittmann.myapplication.main.entity

import com.google.gson.annotations.SerializedName
import com.rittmann.myapplication.main.utils.Logger
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class Position(
    @SerializedName("x")
    var x: Double = 0.0,
    @SerializedName("y")
    var y: Double = 0.0,
) : Logger {

    fun set(x: Double, y: Double) {
        this.x = x
        this.y = y
    }

    fun set(position: Position) {
        this.x = position.x
        this.y = position.y
    }

    fun sum(x: Double, y: Double) {
        this.x += x
        this.y += y
    }

    fun sumNew(x: Double, y: Double): Position {
        return Position(this.x + x, this.y + y)
    }

    fun sumNew(value: Double): Position {
        return Position(this.x + value, this.y + value)
    }

    fun sumNew(position: Position): Position {
        return Position(this.x, this.y).sum(position)
    }

    fun sum(value: Double): Position {
        this.x += value
        this.y += value
        return this
    }

    fun sum(position: Position): Position {
        this.x += position.x
        this.y += position.y
        return this
    }

    fun multiple(l: Double): Position {
        this.x *= l
        this.y *= l
        return this
    }

    fun multiple(x: Double, y: Double): Position {
        this.x *= x
        this.y *= y
        return this
    }

    fun length(): Double {
        return sqrt((x * x) + (y * y))
    }

    fun direction(): Double {
        return atan2(y, x)
    }

    fun angle(): Double {
        return atan2(y, x)
    }

//    Vector2 Vector2::limit_length(const real_t p_len) const {
//        const real_t l = length();
//        Vector2 v = *this;
//        if (l > 0 && p_len < l) {
//            v /= l;
//            v *= p_len;
//        }
//
//        return v;
//    }

    // _directional_vector(vector, directions, deg2rad(simmetry_angle))
    fun _directional_vector(
        vector: Position,
        n_directions: Int,
        simmetry_angle: Double = Math.PI / 2
    ): Position {
        var angle = (vector.angle() + simmetry_angle) / (PI / n_directions)
        angle = if (angle >= 0) floor(angle) else ceil(angle)
        if (abs(angle) % 2 == 1.0) {
            angle = if (angle >= 0) angle + 1 else angle - 1
        }
        angle *= PI / n_directions
        angle -= simmetry_angle
        return Position(cos(angle), sin(angle)).apply { multiple(vector.length()) }
    }

    fun deg_to_rad(p_y: Double): Double {
        return p_y * (Math.PI / 180.0)
    }

    fun normalize(): Position {
        val len = length()

        if (len == 0.0) {
            x = 0.0
            y = 0.0
        } else {
            x /= len
            y /= len
        }

        return this
    }

    fun distance(position: Position): Double {
        return sqrt(
            (position.x - x).pow(2.0) + (position.y - y).pow(2.0)
        )
    }

    companion object {
        fun calculateNormalizedPosition(angle: Double, strength: Double = 1.0): Position {
            return Position(
                cos(angle * Math.PI / 180f) * strength,
                -sin(angle * Math.PI / 180f) * strength, // Is negative to invert the direction
            ).normalize()
        }
    }
}