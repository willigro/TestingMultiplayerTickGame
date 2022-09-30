package com.rittmann.myapplication.main.entity.server

import com.google.gson.annotations.SerializedName

data class PlayerMovementEmit(
    @SerializedName("x") val x: Double,
    @SerializedName("y") val y: Double,
    @SerializedName("angle") val angle: Double,
    @SerializedName("strength") val strength: Double,
    @SerializedName("velocity") val velocity: Double,
)

