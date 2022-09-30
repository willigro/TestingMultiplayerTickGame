package com.rittmann.myapplication.main.server

import com.rittmann.myapplication.main.entity.Player
import com.rittmann.myapplication.main.entity.server.PlayerMovementResult
import io.socket.client.IO
import io.socket.client.Socket
import java.net.URISyntaxException

class ConnectionControl(
    private val connectionControlEvents: ConnectionControlEvents,
) {

    companion object {
        const val EMIT_PLAYER_MOVED = "player moved"
    }

    private var socket: Socket? = null

    fun connect(): ConnectionControl {
        try {
            socket = IO.socket("http://192.168.1.4:3000")
            connectionControlEvents.logCallback("Fine!")
        } catch (e: URISyntaxException) {
            connectionControlEvents.logCallback("Error Connecting to IP! $e")
        }

        socket?.connect()
        configSocketEvents()

        return this
    }

    private fun configSocketEvents() {
        socket?.apply {
            ConnectionControlListeners(this, connectionControlEvents)
        }
    }

    fun emit(tag: String, payload: String) {
        socket?.emit(tag, payload)
    }
}

interface ConnectionControlEvents {
    fun logCallback(log: String)
    fun connectionCreated(player: Player)
    fun newPlayerConnected(player: Player)
    fun playerMovement(playerMovementResult: PlayerMovementResult)
}