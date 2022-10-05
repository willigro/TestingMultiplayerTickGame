package com.rittmann.myapplication.main.match.screen

import com.google.gson.Gson
import com.rittmann.myapplication.main.entity.BULLET_DEFAULT_VELOCITY
import com.rittmann.myapplication.main.entity.Bullet
import com.rittmann.myapplication.main.entity.server.PlayerShootingEmit
import com.rittmann.myapplication.main.entity.server.WorldState
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

    fun update(
        worldState: WorldState
    ) {
        connectionControl.emit(
            ConnectionControl.EMIT_PLAYER_UPDATE, Gson().toJson(
                worldState
            )
        )
    }
}