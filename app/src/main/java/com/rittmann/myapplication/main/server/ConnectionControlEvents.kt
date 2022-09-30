package com.rittmann.myapplication.main.server

import com.rittmann.myapplication.main.entity.Player
import com.rittmann.myapplication.main.entity.server.PlayerMovementResult
import com.rittmann.myapplication.main.entity.server.PlayerMovementWrapResult

interface ConnectionControlEvents {
    fun logCallback(log: String)
    fun connectionCreated(player: Player)
    fun newPlayerConnected(player: Player)
    fun playerMovementWrapResult(playerMovementWrapResult: PlayerMovementWrapResult)
}