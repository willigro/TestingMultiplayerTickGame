package com.rittmann.myapplication.main.entity.server

import com.rittmann.myapplication.main.entity.Bullet
import com.rittmann.myapplication.main.entity.Position
import java.util.concurrent.atomic.AtomicBoolean

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