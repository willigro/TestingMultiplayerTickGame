package com.rittmann.myapplication.main.entity.server

import com.google.gson.annotations.SerializedName
import com.rittmann.myapplication.main.entity.Position

data class PlayerShootingEmit(
    @SerializedName("bulletId") val bulletId: String,
    @SerializedName("ownerId") val ownerId: String,
    @SerializedName("position") val position: Position,
    @SerializedName("angle") val angle: Double,
    @SerializedName("velocity") val velocity: Double,
)