package com.rittmann.myapplication.main.entity.server

import com.google.gson.annotations.SerializedName

data class PlayerAimEmit(
    @SerializedName("angle") val angle: Double,
    @SerializedName("strength") val strength: Double,
)

