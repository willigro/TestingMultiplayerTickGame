package com.rittmann.myapplication.main.match.screen

import com.google.gson.Gson
import com.rittmann.myapplication.main.entity.Player
import com.rittmann.myapplication.main.entity.Position
import com.rittmann.myapplication.main.entity.server.PlayerMovementEmit
import com.rittmann.myapplication.main.server.ConnectionControl

class MatchController(private val connectionControl: ConnectionControl) {

    fun connect() {
        connectionControl.connect()
    }

    fun sendPlayerPosition(position: Position, angle: Double, strength: Double) {
        connectionControl.emit(
            ConnectionControl.EMIT_PLAYER_MOVED, Gson().toJson(
                PlayerMovementEmit(
                    x = position.x,
                    y = position.y,
                    angle = angle,
                    strength = strength,
                    velocity = Player.VELOCITY,
                )
            )
        )
    }
}