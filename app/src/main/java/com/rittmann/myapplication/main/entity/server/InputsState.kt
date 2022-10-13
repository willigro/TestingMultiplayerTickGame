package com.rittmann.myapplication.main.entity.server

import com.rittmann.myapplication.main.entity.Position
import java.util.concurrent.atomic.AtomicBoolean

//fun InputsState.mapToWorldUpdated(): WorldState {
//    val playerUpdate = PlayerUpdate(
//        arrayListOf(
//            PlayerServer(
//                id = this.playerId,
//                playerMovement = PlayerMovement(
//
//                )
//            )
//        )
//    )
//    return WorldState(
//        tick = this.tick,
//        playerUpdate =,
//        bulletUpdate =,
//    )
//}

data class InputWorldState(
    val inputs: ArrayList<InputsState?> = arrayListOf(),
    var start_tick_number: Int = 0,
)

data class InputsState(
    val tick: Int,
    val playerId: String,
    val playerInputsState: PlayerInputsState,
    val bulletInputsState: List<BulletInputsState>,
) {

    @Transient
    private val canSend = AtomicBoolean(true)

    fun thereIsBullet() = bulletInputsState.isNotEmpty()
    fun canSend(): Boolean = canSend.compareAndSet(true, false)
    fun resetCanSend(): InputsState {
        canSend.set(true)
        return this
    }
}

data class PlayerInputsState(
    val playerMovementInputsState: PlayerMovementInputsState,
    val playerAimInputsState: PlayerAimInputsState,
    val playerGunInputsState: PlayerGunInputsState,
)

data class BulletInputsState(
    val bulletId: String,
    val ownerId: String,
)

data class PlayerMovementInputsState(
    val angle: Double,
    val strength: Double,
)

data class PlayerAimInputsState(
    val angle: Double,
    val strength: Double,
)

data class PlayerGunInputsState(
    val position: Position,
    val angle: Double,
)