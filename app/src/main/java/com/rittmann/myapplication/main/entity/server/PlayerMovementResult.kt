package com.rittmann.myapplication.main.entity.server

import com.google.gson.annotations.SerializedName
import com.rittmann.myapplication.main.entity.Position
import java.util.concurrent.atomic.AtomicBoolean

data class PlayerMovementResult(
    @SerializedName("x") val x: Double,
    @SerializedName("y") val y: Double,
    @SerializedName("angle") val angle: Double,
    @SerializedName("strength") val strength: Double,
    @SerializedName("newPosition") val newPosition: Position,
    @SerializedName("velocity") val velocity: Double,
) {
    fun wasPositionApplied(): Boolean {
        return newPositionWasApplied.compareAndSet(false, true)
    }

    private val newPositionWasApplied = AtomicBoolean(false)
}