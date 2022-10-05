package com.rittmann.myapplication.main.server

import com.rittmann.myapplication.main.entity.Player
import com.rittmann.myapplication.main.entity.server.WorldState

interface ConnectionControlEvents {
    fun logCallback(log: String)
    fun connectionCreated(player: Player)
    fun newPlayerConnected(player: ConnectionControlListeners.NewPlayerConnected)
    fun playerDisconnected(id: String)
    fun onPlayerUpdate(worldState: WorldState)
    fun onPlayerUpdate(worldState: List<WorldState>)
}