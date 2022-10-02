package com.rittmann.myapplication.main.entity.server

import com.google.gson.annotations.SerializedName
import com.rittmann.myapplication.main.entity.Position

data class PlayerShootingEmit(
    @SerializedName("id") val id: String,
    @SerializedName("bullet_position") val position: Position,
    @SerializedName("angle") val angle: Double,
    @SerializedName("velocity") val velocity: Double,
)