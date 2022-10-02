package com.rittmann.myapplication.main.server

import com.rittmann.myapplication.main.utils.Logger
import io.socket.client.IO
import io.socket.client.Socket
import java.net.URISyntaxException

class ConnectionControl(
    private val connectionControlEvents: ConnectionControlEvents,
) : Logger {

    companion object {
        const val EMIT_PLAYER_MOVEMENT = "player movement"
        const val EMIT_PLAYER_SHOOTING = "player shooting"
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

    fun disconnect(): ConnectionControl {
        try {
            socket?.disconnect()
            connectionControlEvents.logCallback("Disconnecting!")
        } catch (e: URISyntaxException) {
            e.printStackTrace()
            connectionControlEvents.logCallback("Error Disconnecting to IP! $e")
        }

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

