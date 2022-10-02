package com.rittmann.myapplication.main.server

import com.rittmann.myapplication.main.entity.Bullet
import com.rittmann.myapplication.main.entity.Player
import com.rittmann.myapplication.main.entity.Position
import com.rittmann.myapplication.main.entity.server.PlayerAimResult
import com.rittmann.myapplication.main.entity.server.PlayerMovementResult
import com.rittmann.myapplication.main.entity.server.PlayerMovementWrapResult
import com.rittmann.myapplication.main.entity.server.PlayerShootingResponseWrap
import org.json.JSONObject

fun JSONObject.mapToPlayerMovementResult(): PlayerMovementWrapResult {
    val playerMovementResultJson = this.getJSONObject(DATA_PLAYER_MOVEMENT)
    val playerAimResultJson = this.getJSONObject(DATA_PLAYER_AIM)

    val newPosition = playerMovementResultJson.getJSONObject(DATA_PLAYER_MOVEMENT_NEW_POSITION)

    val playerMovementResult = PlayerMovementResult(
        x = playerMovementResultJson.getDouble(DATA_PLAYER_POSITION_X),
        y = playerMovementResultJson.getDouble(DATA_PLAYER_POSITION_Y),
        angle = playerMovementResultJson.getDouble(DATA_PLAYER_MOVEMENT_ANGLE),
        strength = playerMovementResultJson.getDouble(DATA_PLAYER_MOVEMENT_STRENGTH),
        newPosition = Position(
            x = newPosition.getDouble(DATA_PLAYER_POSITION_X),
            y = newPosition.getDouble(DATA_PLAYER_POSITION_Y),
        ),
        velocity = playerMovementResultJson.getDouble(DATA_PLAYER_MOVEMENT_VELOCITY),
    )

    val playerAimResult = PlayerAimResult(
        angle = playerAimResultJson.getDouble(DATA_PLAYER_MOVEMENT_ANGLE),
        strength = playerAimResultJson.getDouble(DATA_PLAYER_MOVEMENT_STRENGTH),
    )

    return PlayerMovementWrapResult(
        id = this.getString(DATA_PLAYER_ID),
        playerMovementResult = playerMovementResult,
        playerAimResult = playerAimResult,
    )
}

fun JSONObject.mapToPlayer(): Player {
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

fun JSONObject.mapToPlayerShootingResponseWrap(): PlayerShootingResponseWrap {
    val positionJson = this.getJSONObject(DATA_PLAYER_POSITION)
    val bullet = Bullet(
        bulletId = "",
        position = Position(
            x = positionJson.getDouble(DATA_PLAYER_POSITION_X),
            y = positionJson.getDouble(DATA_PLAYER_POSITION_Y),
        ),
        angle = this.getDouble(DATA_PLAYER_MOVEMENT_ANGLE),
    )

    return PlayerShootingResponseWrap(
        this.getString(DATA_PLAYER_ID),
        bullet,
    )
}