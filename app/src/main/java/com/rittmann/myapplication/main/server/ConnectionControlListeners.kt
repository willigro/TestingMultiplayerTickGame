package com.rittmann.myapplication.main.server

import com.rittmann.myapplication.main.entity.Player
import com.rittmann.myapplication.main.utils.Logger
import io.socket.client.Socket
import org.json.JSONException
import org.json.JSONObject

const val ON_NEW_PLAYER_CONNECTED = "new player connected"
const val ON_PLAYER_CREATED = "player created"
const val ON_PLAYER_DISCONNECTED = "player disconnected"
const val ON_WORLD_STATE = "world state"

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
const val DATA_PLAYER_MOVEMENT_VELOCITY = "velocity"
const val DATA_PLAYER_AIM = "playerAim"

class ConnectionControlListeners(
    private val socket: Socket,
    private val connectionControlEvents: ConnectionControlEvents
) : Logger {

    init {
        onEventConnect()
        onEventDisconnect()

        onPlayerCreated()
        onNewPlayerConnected()
        onPlayerDisconnected()

        onWorldUpdated()
    }

    private fun onEventConnect() = with(socket) {
        on(Socket.EVENT_CONNECT) {
            connectionControlEvents.logCallback("EVENT_CONNECT")
        }
    }

    private fun onEventDisconnect() = with(socket) {
        on(Socket.EVENT_DISCONNECT) {
            connectionControlEvents.logCallback("EVENT_DISCONNECT")
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
                                NewPlayerConnected(
                                    player = newPlayerJson.mapToPlayer(),
                                    tick = data.getInt("tick"),
                                )
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
                connectionControlEvents.newPlayerConnected(
                    NewPlayerConnected(
                        player = newPlayerJson.mapToPlayer(),
                        tick = data.getInt("tick"),
                    )
                )
            } catch (e: JSONException) {
                e.printStackTrace()
                connectionControlEvents.logCallback("NEW_PLAYER Error getting ID")
            }
        }
    }

    class NewPlayerConnected(val player: Player, val tick: Int)

    private fun onPlayerDisconnected() = with(socket) {
        on(ON_PLAYER_DISCONNECTED) { args ->
            val data = args[0] as JSONObject
            try {
                val id = data.getString(DATA_PLAYER_ID)
                val players = data.getString(DATA_PLAYERS)
                connectionControlEvents.logCallback("PLAYER_DISCONNECTED! $id\nRemain Player: $players")
                connectionControlEvents.playerDisconnected(id)
            } catch (e: JSONException) {
                e.printStackTrace()
                connectionControlEvents.logCallback("PLAYER_DISCONNECTED Error!")
            }
        }
    }

    private fun onWorldUpdated() = with(socket) {
        on(ON_WORLD_STATE) { args ->
            if (args != null && args.isNotEmpty()) {
                val data = args[0] as JSONObject
                try {
//                connectionControlEvents.logCallback("ON_PLAYER_SHOOTING Shooting result: $result")
                    connectionControlEvents.onPlayerUpdate(data.mapToListWorldUpdate())
//                    connectionControlEvents.onPlayerUpdate(data.mapToWorldUpdate())
                } catch (e: JSONException) {
                    e.printStackTrace()
                    connectionControlEvents.logCallback("ON_PLAYER_MOVED Error")
                }
            }
        }
    }
}