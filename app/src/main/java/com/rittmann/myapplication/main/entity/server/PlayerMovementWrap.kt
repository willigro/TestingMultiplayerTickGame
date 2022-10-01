package com.rittmann.myapplication.main.entity.server

import com.google.gson.annotations.SerializedName

data class PlayerMovementWrapEmit(
    @SerializedName("playerMovement") val playerMovementEmit: PlayerMovementEmit,
    @SerializedName("playerAim") val playerAimEmit: PlayerAimEmit,
)

data class PlayerMovementWrapResult(
    @SerializedName("id") val id: String,
    @SerializedName("playerMovement") val playerMovementResult: PlayerMovementResult,
    @SerializedName("playerAim") val playerAimResult: PlayerAimResult,
)

fun PlayerMovementWrapResult?.wasPositionApplied(): Boolean {
    return this?.playerMovementResult?.wasPositionApplied() == true
}

fun PlayerMovementWrapResult?.wasAimApplied(): Boolean {
    return this?.playerAimResult?.wasAimApplied() == true
}