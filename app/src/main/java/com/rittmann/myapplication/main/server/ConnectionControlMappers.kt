package com.rittmann.myapplication.main.server

import com.rittmann.myapplication.main.entity.BULLET_DEFAULT_MAX_DISTANCE
import com.rittmann.myapplication.main.entity.BULLET_DEFAULT_VELOCITY
import com.rittmann.myapplication.main.entity.Bullet
import com.rittmann.myapplication.main.entity.Player
import com.rittmann.myapplication.main.entity.Position
import com.rittmann.myapplication.main.entity.server.PlayerAimResult
import com.rittmann.myapplication.main.entity.server.PlayerMovementResult
import com.rittmann.myapplication.main.entity.server.PlayerMovementWrapResult
import com.rittmann.myapplication.main.entity.server.PlayerShootingResponseWrap
import java.util.concurrent.atomic.AtomicBoolean
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
        ownerId = "",
        position = Position(
            x = positionJson.getDouble(DATA_PLAYER_POSITION_X),
            y = positionJson.getDouble(DATA_PLAYER_POSITION_Y),
        ),
        angle = this.getDouble(DATA_PLAYER_MOVEMENT_ANGLE),
        velocity = BULLET_DEFAULT_VELOCITY,
        maxDistance = BULLET_DEFAULT_MAX_DISTANCE,
    )

    return PlayerShootingResponseWrap(
        this.getString(DATA_PLAYER_ID),
        bullet,
    )
}


fun JSONObject.mapToWorldUpdate(): WorldState {

    val players = arrayListOf<PlayerServer>()
    val playerListJson = this.getJSONArray("players")

    for (i in 0 until playerListJson.length()) {
        val playerJson = playerListJson.getJSONObject(i)

        val playerMovementResultJson = playerJson.getJSONObject(DATA_PLAYER_MOVEMENT)
        val playerAimResultJson = playerJson.getJSONObject(DATA_PLAYER_AIM)

        val positionJson = playerMovementResultJson.getJSONObject(DATA_PLAYER_POSITION)

        players.add(
            PlayerServer(
                id = playerJson.getString(DATA_PLAYER_ID),
                playerMovement = PlayerMovement(
                    position = Position(
                        x = positionJson.getDouble(DATA_PLAYER_POSITION_X),
                        y = positionJson.getDouble(DATA_PLAYER_POSITION_Y),
                    ),
                    angle = playerMovementResultJson.getDouble(DATA_PLAYER_MOVEMENT_ANGLE),
                    strength = playerMovementResultJson.getDouble(DATA_PLAYER_MOVEMENT_STRENGTH),
                    velocity = playerMovementResultJson.getDouble(DATA_PLAYER_MOVEMENT_VELOCITY),
                ),
                playerAim = PlayerAim(
                    angle = playerAimResultJson.getDouble(DATA_PLAYER_MOVEMENT_ANGLE),
                    strength = playerAimResultJson.getDouble(DATA_PLAYER_MOVEMENT_STRENGTH),
                )
            )
        )
    }

    val bullets = arrayListOf<Bullet>()
    val bulletListJson = this.getJSONArray("bullets")
    for (i in 0 until bulletListJson.length()) {
        val bulletJson = bulletListJson.getJSONObject(i)

        val positionJson = bulletJson.getJSONObject(DATA_PLAYER_POSITION)

        bullets.add(
            Bullet(
                bulletId = bulletJson.getString("id"),
                ownerId = bulletJson.getString("owner"),
                position = Position(
                    x = positionJson.getDouble(DATA_PLAYER_POSITION_X),
                    y = positionJson.getDouble(DATA_PLAYER_POSITION_Y),
                ),
                angle = bulletJson.getDouble(DATA_PLAYER_MOVEMENT_ANGLE),
                maxDistance = bulletJson.getDouble("maxDistance"),
                velocity = bulletJson.getDouble("velocity"),
            ).apply {
                ownerId = bulletJson.getString("owner")
            }
        )
    }

    return WorldState(
        playerUpdate = PlayerUpdate(players = players),
        bulletUpdate = BulletUpdate(bullets = bullets),
    )
}

class WorldState(
    val playerUpdate: PlayerUpdate,
    val bulletUpdate: BulletUpdate,
)

class BulletUpdate(
    val bullets: List<Bullet>
)

class PlayerUpdate(
    val players: List<PlayerServer>
)

class PlayerServer(
    val id: String,
    val playerMovement: PlayerMovement,
    val playerAim: PlayerAim,
)

data class PlayerAim(
    val angle: Double,
    val strength: Double,
) {
    fun wasAimApplied(): Boolean {
        return newPositionWasApplied.compareAndSet(false, true)
    }

    fun resetAppliedAim() {
        newPositionWasApplied.set(false)
    }

    private val newPositionWasApplied = AtomicBoolean(false)
}

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

fun PlayerServer?.wasPositionMovementApplied(): Boolean {
    return this?.playerMovement?.wasPositionApplied() == true
}

fun PlayerServer?.wasAimApplied(): Boolean {
    return this?.playerAim?.wasAimApplied() == true
}