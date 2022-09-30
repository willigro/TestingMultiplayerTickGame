package com.rittmann.myapplication.main.match.screen

import com.google.gson.Gson
import com.rittmann.myapplication.main.entity.server.PlayerAimEmit
import com.rittmann.myapplication.main.entity.server.PlayerMovementEmit
import com.rittmann.myapplication.main.entity.server.PlayerMovementWrapEmit
import com.rittmann.myapplication.main.server.ConnectionControl

class MatchController(private val connectionControl: ConnectionControl) {

    fun connect() {
        connectionControl.connect()
    }

    fun sendPlayerPosition(playerMovementEmit: PlayerMovementEmit, playerAimEmit: PlayerAimEmit) {
        connectionControl.emit(
            ConnectionControl.EMIT_PLAYER_MOVEMENT, Gson().toJson(
                PlayerMovementWrapEmit(
                    playerMovementEmit = playerMovementEmit,
                    playerAimEmit = playerAimEmit,
                )
            )
        )
    }
}