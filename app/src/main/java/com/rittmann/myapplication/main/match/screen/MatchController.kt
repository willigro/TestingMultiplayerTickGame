package com.rittmann.myapplication.main.match.screen

import com.google.gson.Gson
import com.rittmann.myapplication.main.entity.BULLET_DEFAULT_VELOCITY
import com.rittmann.myapplication.main.entity.Bullet
import com.rittmann.myapplication.main.entity.server.PlayerAimEmit
import com.rittmann.myapplication.main.entity.server.PlayerMovementEmit
import com.rittmann.myapplication.main.entity.server.PlayerMovementWrapEmit
import com.rittmann.myapplication.main.entity.server.PlayerShootingEmit
import com.rittmann.myapplication.main.server.ConnectionControl

class MatchController(private val connectionControl: ConnectionControl) {

    fun connect() {
        connectionControl.connect()
    }

    fun disconnect() {
        connectionControl.disconnect()
    }

    fun shoot(bullet: Bullet) {
        connectionControl.emit(
            ConnectionControl.EMIT_PLAYER_SHOOTING, Gson().toJson(
                PlayerShootingEmit(
                    bulletId = bullet.bulletId,
                    ownerId = bullet.ownerId,
                    position = bullet.position,
                    angle = bullet.angle,
                    velocity = BULLET_DEFAULT_VELOCITY,
                )
            )
        )
    }

    fun update(playerMovementEmit: PlayerMovementEmit, playerAimEmit: PlayerAimEmit) {
        connectionControl.emit(
            ConnectionControl.EMIT_PLAYER_UPDATE, Gson().toJson(
                PlayerMovementWrapEmit(
                    playerMovementEmit = playerMovementEmit,
                    playerAimEmit = playerAimEmit,
                )
            )
        )
    }
}