package com.rittmann.myapplication.main.server

import com.rittmann.myapplication.main.entity.server.WorldState

interface ConnectionControlEvents {
    fun logCallback(log: String)
    fun connectionCreated(player: ConnectionControlListeners.NewPlayerConnected)
    fun newPlayerConnected(player: ConnectionControlListeners.NewPlayerConnected)
    fun onGameStarted(tick: Int)
    fun onGameDisconnected()
    fun playerDisconnected(id: String)
    fun onPlayerUpdate(worldState: List<WorldState>)
}