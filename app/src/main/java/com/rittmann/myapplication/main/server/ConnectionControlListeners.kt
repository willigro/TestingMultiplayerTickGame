package com.rittmann.myapplication.main.server

import io.socket.client.Socket
import org.json.JSONException
import org.json.JSONObject

const val ON_NEW_PLAYER_CONNECTED = "new player connected"
const val ON_PLAYER_CREATED = "player created"
const val ON_PLAYER_DISCONNECTED = "player disconnected"
const val ON_PLAYER_MOVED = "players moved"

const val DATA_PLAYER_ID = "id"
const val DATA_PLAYER_POSITION = "position"
const val DATA_PLAYER_POSITION_X = "x"
const val DATA_PLAYER_POSITION_Y = "y"
const val DATA_PLAYER_COLOR = "color"
const val DATA_PLAYERS = "players"
const val DATA_NEW_PLAYER = "newPlayer"
const val DATA_PLAYER_MOVEMENT = "playerMovement"
const val DATA_PLAYER_MOVEMENT_ANGLE = "angle"
const val DATA_PLAYER_MOVEMENT_STRENGTH = "strength"
const val DATA_PLAYER_MOVEMENT_NEW_POSITION = "newPosition"

class ConnectionControlListeners(
    private val socket: Socket,
    private val connectionControlEvents: ConnectionControlEvents
) {

    init {
        onEventConnect()
        onEventDisconnect()

        onPlayerCreated()
        onNewPlayerConnected()
        onPlayerDisconnected()
        onPlayerMoved()
    }

    private fun onEventConnect() = with(socket) {
        on(Socket.EVENT_CONNECT) {
            connectionControlEvents.logCallback("EVENT_CONNECT")
        }
    }

    private fun onEventDisconnect() = with(socket) {
        on(Socket.EVENT_DISCONNECT) {
            connectionControlEvents.logCallback("EVENT_DISCONNECT")
            socket.emit("message", "hi")
        }
    }

    private fun onPlayerCreated() = with(socket) {
        on(ON_PLAYER_CREATED) { args ->
            val data = args[0] as JSONObject
            try {
                val newPlayerJson = data.getJSONObject(DATA_NEW_PLAYER)
                val players = data.getJSONArray(DATA_PLAYERS)

                val ownPlayer = newPlayerJson.mapToPlayer()

                connectionControlEvents.logCallback("PLAYER_CREATED My Player: $ownPlayer\nPlayers: $players")
                connectionControlEvents.connectionCreated(ownPlayer)

                for (i in 0 until players.length()) {
                    players.getJSONObject(i)?.also { json ->
                        val newCreatedPlayer = json.mapToPlayer()
                        if (newCreatedPlayer.playerId != ownPlayer.playerId) {
                            connectionControlEvents.newPlayerConnected(
                                newCreatedPlayer
                            )
                        }
                    }
                }
            } catch (e: JSONException) {
                e.printStackTrace()
                connectionControlEvents.logCallback("PLAYER_CREATED Error getting ID")
            }
        }
    }

    private fun onNewPlayerConnected() = with(socket) {
        on(ON_NEW_PLAYER_CONNECTED) { args ->
            val data = args[0] as JSONObject
            try {
                val players = data.getString(DATA_PLAYERS)
                val newPlayerJson = data.getJSONObject(DATA_NEW_PLAYER)

                connectionControlEvents.logCallback("NEW_PLAYER Players: $players\nNew player: $newPlayerJson")
                connectionControlEvents.newPlayerConnected(newPlayerJson.mapToPlayer())
            } catch (e: JSONException) {
                e.printStackTrace()
                connectionControlEvents.logCallback("NEW_PLAYER Error getting ID")
            }
        }
    }

    private fun onPlayerDisconnected() = with(socket) {
        on(ON_PLAYER_DISCONNECTED) { args ->
            val data = args[0] as JSONObject
            try {
                val id = data.getString(DATA_PLAYER_ID)
                val players = data.getString(DATA_PLAYERS)
                connectionControlEvents.logCallback("PLAYER_DISCONNECTED! $id\nRemain Player: $players")
            } catch (e: JSONException) {
                e.printStackTrace()
                connectionControlEvents.logCallback("PLAYER_DISCONNECTED Error!")
            }
        }
    }

    private fun onPlayerMoved() = with(socket) {
        on(ON_PLAYER_MOVED) { args ->
            val data = args[0] as JSONObject
            try {
                val playerMovement = data.getJSONObject(DATA_PLAYER_MOVEMENT)

                connectionControlEvents.logCallback("ON_PLAYER_MOVED Player Movement: $playerMovement")
                connectionControlEvents.playerMovement(playerMovement.mapToPlayerMovementResult())
            } catch (e: JSONException) {
                e.printStackTrace()
                connectionControlEvents.logCallback("ON_PLAYER_MOVED Error")
            }
        }
    }
}