package com.rittmann.myapplication.main.server

import com.rittmann.myapplication.main.entity.Player
import com.rittmann.myapplication.main.entity.server.PlayerMovementWrapResult
import com.rittmann.myapplication.main.entity.server.PlayerShootingResponseWrap

interface ConnectionControlEvents {
    fun logCallback(log: String)
    fun connectionCreated(player: Player)
    fun newPlayerConnected(player: Player)
    fun playerMovementWrapResult(playerMovementWrapResult: PlayerMovementWrapResult)
    fun playerDisconnected(id: String)
    fun onPlayerEnemyShooting(shootingResponseWrap: PlayerShootingResponseWrap)
    fun onPlayerUpdate(worldState: WorldState)
}