package com.rittmann.myapplication.main.server

import android.util.Log
import com.rittmann.myapplication.main.entity.Bullet
import com.rittmann.myapplication.main.entity.Player
import com.rittmann.myapplication.main.entity.Position
import com.rittmann.myapplication.main.entity.server.BulletUpdate
import com.rittmann.myapplication.main.entity.server.PlayerAim
import com.rittmann.myapplication.main.entity.server.PlayerGunPointer
import com.rittmann.myapplication.main.entity.server.PlayerMovement
import com.rittmann.myapplication.main.entity.server.PlayerServer
import com.rittmann.myapplication.main.entity.server.PlayerUpdate
import com.rittmann.myapplication.main.entity.server.WorldState
import com.rittmann.myapplication.main.match.screen.GLOBAL_TAG
import org.json.JSONObject

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

fun JSONObject.mapToWorldUpdate(): WorldState {
//    Log.i(GLOBAL_TAG, "tick=${this.getInt("tick")}")

    val players = arrayListOf<PlayerServer>()
    val playerListJson = this.getJSONArray("players")

    for (i in 0 until playerListJson.length()) {
        val playerJson = playerListJson.getJSONObject(i)

        val playerMovementResultJson = playerJson.getJSONObject(DATA_PLAYER_MOVEMENT)
        val playerAimResultJson = playerJson.getJSONObject(DATA_PLAYER_AIM)
        val playerGunPointerJson = playerJson.getJSONObject("playerGunPointer")
        val playerGunPointerPositionJson = playerGunPointerJson.getJSONObject("position")

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
                ),
                playerGunPointer = PlayerGunPointer(
                    position = Position(
                        x = playerGunPointerPositionJson.getDouble(DATA_PLAYER_POSITION_X),
                        y = playerGunPointerPositionJson.getDouble(DATA_PLAYER_POSITION_Y),
                    ),
                    angle = playerGunPointerJson.getDouble(DATA_PLAYER_MOVEMENT_ANGLE),
                ),
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

//    if (bullets.size > 0) {
//        Log.i(GLOBAL_TAG, "tick=${this.getInt("tick")} bullet=${bullets.size}")
//    }

//    Log.i(GLOBAL_TAG, "bullets.size=${bullets.size}")

    return WorldState(
        tick = this.getInt("tick"),
        playerUpdate = PlayerUpdate(players = players),
        bulletUpdate = BulletUpdate(bullets = bullets),
    )
}


fun JSONObject.mapToListWorldUpdate(): List<WorldState> {

    val result = arrayListOf<WorldState>()
    val arr = this.getJSONArray("response")
//    Log.i(GLOBAL_TAG, "init response")
    for (i in 0 until arr.length()) {
        result.add(arr.getJSONObject(i).mapToWorldUpdate())
    }

    return result
}