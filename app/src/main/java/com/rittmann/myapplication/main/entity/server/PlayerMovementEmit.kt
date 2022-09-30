package com.rittmann.myapplication.main.entity.server

import com.google.gson.annotations.SerializedName
import com.rittmann.myapplication.main.entity.Position
import java.util.concurrent.atomic.AtomicBoolean

data class PlayerMovementEmit(
    @SerializedName("x") val x: Double,
    @SerializedName("y") val y: Double,
    @SerializedName("angle") val angle: Double,
    @SerializedName("strength") val strength: Double,
    @SerializedName("velocity") val velocity: Double,
)

data class PlayerMovementResult(
    @SerializedName("id") val id: String,
    @SerializedName("x") val x: Double,
    @SerializedName("y") val y: Double,
    @SerializedName("angle") val angle: Double,
    @SerializedName("strength") val strength: Double,
    @SerializedName("newPosition") val newPosition: Position,
    @SerializedName("velocity") val velocity: Double,
) {
    fun newPositionWasApplied(): Boolean {
        return newPositionApplied.compareAndSet(false, true)
    }

    val newPositionApplied = AtomicBoolean(false)
}