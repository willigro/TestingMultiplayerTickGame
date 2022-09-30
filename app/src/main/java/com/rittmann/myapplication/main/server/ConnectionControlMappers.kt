package com.rittmann.myapplication.main.server

import com.rittmann.myapplication.main.entity.Player
import com.rittmann.myapplication.main.entity.Position
import com.rittmann.myapplication.main.entity.server.PlayerMovementResult
import org.json.JSONObject

fun JSONObject.mapToPlayerMovementResult() : PlayerMovementResult {
    val newPosition = this.getJSONObject(DATA_PLAYER_MOVEMENT_NEW_POSITION)

    return PlayerMovementResult(
        id = this.getString(DATA_PLAYER_ID),
        x = this.getDouble(DATA_PLAYER_POSITION_X),
        y = this.getDouble(DATA_PLAYER_POSITION_Y),
        angle = this.getDouble(DATA_PLAYER_MOVEMENT_ANGLE),
        strength = this.getDouble(DATA_PLAYER_MOVEMENT_STRENGTH),
        newPosition = Position(
            x = newPosition.getDouble(DATA_PLAYER_POSITION_X),
            y = newPosition.getDouble(DATA_PLAYER_POSITION_Y),
        ),
    )
}

fun JSONObject.mapToPlayer() : Player {
    val positionJson = this.getJSONObject(DATA_PLAYER_POSITION)
    val player = Player(
        playerId = this.getString(DATA_PLAYER_ID),
        position = Position(
            x = positionJson.getDouble(DATA_PLAYER_POSITION_X),
            y = positionJson.getDouble(DATA_PLAYER_POSITION_Y),
        ),
        color = this.getString(DATA_PLAYER_COLOR),
    )

    return player
}