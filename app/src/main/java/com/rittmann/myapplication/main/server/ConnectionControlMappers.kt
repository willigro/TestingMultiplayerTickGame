package com.rittmann.myapplication.main.server

import com.rittmann.myapplication.main.entity.Bullet
import com.rittmann.myapplication.main.entity.Player
import com.rittmann.myapplication.main.entity.Position
import com.rittmann.myapplication.main.entity.server.PlayerAimResult
import com.rittmann.myapplication.main.entity.server.PlayerMovementResult
import com.rittmann.myapplication.main.entity.server.PlayerMovementWrapResult
import com.rittmann.myapplication.main.entity.server.PlayerShootingResponseWrap
import java.util.concurrent.atomic.AtomicBoolean
import org.json.JSONArray
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
    val playerMovementJson = this.getJSONObject(DATA_PLAYER_MOVEMENT)
    val positionJson = playerMovementJson.getJSONObject(DATA_PLAYER_POSITION)

    return Player(
        playerId = this.getString(DATA_PLAYER_ID),
        position = Position(
            x = positionJson.getDouble(DATA_PLAYER_POSITION_X),
            y = positionJson.getDouble(DATA_PLAYER_POSITION_Y),
        ),
        color = this.getString(DATA_PLAYER_COLOR),
    )
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


fun JSONArray.mapToPlayerUpdate(): PlayerUpdate {
    val list = arrayListOf<PlayerServer>()

    for (i in 0 until this.length()) {
        val json = this.getJSONObject(i)
        val playerMovementResultJson = json.getJSONObject(DATA_PLAYER_MOVEMENT)

        val positionJson = playerMovementResultJson.getJSONObject(DATA_PLAYER_POSITION)

        list.add(
            PlayerServer(
                id = json.getString(DATA_PLAYER_ID),
                PlayerMovement(
                    position = Position(
                        x = positionJson.getDouble(DATA_PLAYER_POSITION_X),
                        y = positionJson.getDouble(DATA_PLAYER_POSITION_Y),
                    ),
                    angle = playerMovementResultJson.getDouble(DATA_PLAYER_MOVEMENT_ANGLE),
                    strength = playerMovementResultJson.getDouble(DATA_PLAYER_MOVEMENT_STRENGTH),
                    velocity = playerMovementResultJson.getDouble(DATA_PLAYER_MOVEMENT_VELOCITY),
                ),
            )
        )
    }

    return PlayerUpdate(
        players = list
    )
}

class PlayerUpdate(
    val players: List<PlayerServer>
)

class PlayerServer(
    val id: String,
    val playerMovement: PlayerMovement,
)

data class PlayerMovement(
    val position: Position,
    val angle: Double,
    val strength: Double,
    val velocity: Double,
) {
    fun wasPositionApplied(): Boolean {
        return newPositionWasApplied.compareAndSet(false, true)
    }

    fun resetPositionWasApplied() {
        newPositionWasApplied.set(false)
    }

    private val newPositionWasApplied = AtomicBoolean(false)
}

fun PlayerMovement?.wasPositionAppliedExt(): Boolean {
    return this?.wasPositionApplied() == true
}