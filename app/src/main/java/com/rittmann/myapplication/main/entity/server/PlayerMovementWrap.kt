package com.rittmann.myapplication.main.entity.server

import com.google.gson.annotations.SerializedName

data class PlayerMovementWrapEmit(
    @SerializedName("playerMovement") val playerMovementEmit: PlayerMovementEmit,
    @SerializedName("playerAim") val playerAimEmit: PlayerAimEmit,
)

