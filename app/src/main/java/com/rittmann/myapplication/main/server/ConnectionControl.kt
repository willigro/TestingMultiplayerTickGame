package com.rittmann.myapplication.main.server

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.rittmann.myapplication.main.entity.Player
import com.rittmann.myapplication.main.entity.Position
import com.rittmann.myapplication.main.entity.server.PlayerMovementResult
import com.rittmann.myapplication.main.match.screen.GLOBAL_TAG
import io.socket.client.IO
import io.socket.client.Socket
import java.net.URISyntaxException
import org.json.JSONException
import org.json.JSONObject

class ConnectionControl(
    private val context: AppCompatActivity,
    private val connectionControlEvents: ConnectionControlEvents,
) {

    companion object {
        const val ON_NEW_PLAYER_CONNECTED = "new player connected"
        const val ON_PLAYER_CREATED = "player created"
        const val ON_PLAYER_DISCONNECTED = "player disconnected"
        const val ON_PLAYER_MOVED = "players moved"

        const val EMIT_PLAYER_MOVED = "player moved"

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
    }

    private var socket: Socket? = null

    init {
        try {
            socket = IO.socket("http://192.168.1.4:3000")
            connectionControlEvents.logCallback("Fine!")
        } catch (e: URISyntaxException) {
            connectionControlEvents.logCallback("Error Connecting to IP! $e")
        }
    }

    fun connect(): ConnectionControl {
        socket?.connect()
        configSocketEvents()

        return this
    }

    private fun configSocketEvents() {
        setupConnectionEvents()
        setupPlayerEvents()
    }

    private fun setupPlayerEvents() {
        socket?.apply {
            on(ON_PLAYER_CREATED) { args ->
                val data = args[0] as JSONObject
                try {
                    val newPlayerJson = data.getJSONObject(DATA_NEW_PLAYER)
                    val players = data.getJSONArray(DATA_PLAYERS)

                    val ownPlayer = extractPlayer(newPlayerJson)

                    connectionControlEvents.logCallback("PLAYER_CREATED My Player: $ownPlayer\nPlayers: $players")
                    connectionControlEvents.connectionCreated(ownPlayer)

                    for (i in 0 until players.length()) {
                        players.getJSONObject(i)?.also { json ->
                            val newCreatedPlayer = extractPlayer(json)
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

            on(ON_PLAYER_MOVED) { args ->
                val data = args[0] as JSONObject
                try {
                    val playerMovement = data.getJSONObject(DATA_PLAYER_MOVEMENT)

                    connectionControlEvents.logCallback("ON_PLAYER_MOVED Player Movement: $playerMovement")
                    connectionControlEvents.playerMovement(
                        extractPlayerMovement(
                            playerMovement
                        )
                    )
                } catch (e: JSONException) {
                    e.printStackTrace()
                    connectionControlEvents.logCallback("ON_PLAYER_MOVED Error")
                }
            }

            on(ON_NEW_PLAYER_CONNECTED) { args ->
                val data = args[0] as JSONObject
                try {
                    val players = data.getString(DATA_PLAYERS)
                    val newPlayerJson = data.getJSONObject(DATA_NEW_PLAYER)

                    connectionControlEvents.logCallback("NEW_PLAYER Players: $players\nNew player: $newPlayerJson")
                    connectionControlEvents.newPlayerConnected(
                        extractPlayer(
                            newPlayerJson
                        )
                    )
                } catch (e: JSONException) {
                    e.printStackTrace()
                    connectionControlEvents.logCallback("NEW_PLAYER Error getting ID")
                }
            }

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
    }

    private fun setupConnectionEvents() {
        socket?.apply {
            on(Socket.EVENT_CONNECT) {
                connectionControlEvents.logCallback("EVENT_CONNECT")
            }

            on(Socket.EVENT_DISCONNECT) {
                connectionControlEvents.logCallback("EVENT_DISCONNECT")
                socket?.emit("message", "hi")
            }
        }
    }

    private fun extractPlayer(playerJson: JSONObject): Player {
        val positionJson = playerJson.getJSONObject(DATA_PLAYER_POSITION)
        val player = Player(
            playerId = playerJson.getString(DATA_PLAYER_ID),
            position = Position(
                x = positionJson.getDouble(DATA_PLAYER_POSITION_X),
                y = positionJson.getDouble(DATA_PLAYER_POSITION_Y),
            ),
            color = playerJson.getString(DATA_PLAYER_COLOR),
        )

        return player
    }

    private fun extractPlayerMovement(playerMovementJson: JSONObject): PlayerMovementResult {
        Log.i(GLOBAL_TAG, playerMovementJson.toString())

        val newPosition = playerMovementJson.getJSONObject(DATA_PLAYER_MOVEMENT_NEW_POSITION)

        return PlayerMovementResult(
            id = playerMovementJson.getString(DATA_PLAYER_ID),
            x = playerMovementJson.getDouble(DATA_PLAYER_POSITION_X),
            y = playerMovementJson.getDouble(DATA_PLAYER_POSITION_Y),
            angle = playerMovementJson.getDouble(DATA_PLAYER_MOVEMENT_ANGLE),
            strength = playerMovementJson.getDouble(DATA_PLAYER_MOVEMENT_STRENGTH),
            newPosition = Position(
                x = newPosition.getDouble(DATA_PLAYER_POSITION_X),
                y = newPosition.getDouble(DATA_PLAYER_POSITION_Y),
            ),
        )
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