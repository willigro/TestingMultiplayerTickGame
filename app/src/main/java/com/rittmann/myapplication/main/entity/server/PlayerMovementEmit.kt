package com.rittmann.myapplication.main.entity.server

import com.google.gson.annotations.SerializedName
import com.rittmann.myapplication.main.entity.Position

data class PlayerMovementEmit(
    @SerializedName("position") val position: Position,
    @SerializedName("angle") val angle: Double,
    @SerializedName("strength") val strength: Double,
    @SerializedName("velocity") val velocity: Double,
)

