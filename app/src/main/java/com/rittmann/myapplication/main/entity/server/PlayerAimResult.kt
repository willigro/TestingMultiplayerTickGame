package com.rittmann.myapplication.main.entity.server

import com.google.gson.annotations.SerializedName
import java.util.concurrent.atomic.AtomicBoolean

data class PlayerAimResult(
    @SerializedName("angle") val angle: Double,
    @SerializedName("strength") val strength: Double,
) {
    fun wasAimApplied(): Boolean {
        return newAimWasApplied.compareAndSet(false, true)
    }

    val newAimWasApplied = AtomicBoolean(false)
}