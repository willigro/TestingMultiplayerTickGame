package com.rittmann.myapplication.main.entity.server

import com.rittmann.myapplication.main.entity.Bullet
import com.rittmann.myapplication.main.entity.Position
import com.rittmann.myapplication.main.utils.Logger
import java.util.concurrent.atomic.AtomicBoolean

data class WorldState(
    val tick: Int,
    val playerUpdate: PlayerUpdate,
    val bulletUpdate: BulletUpdate?,
) : Logger {

    @Transient
    val alreadyProcessed: AtomicBoolean = AtomicBoolean(false)

    fun printPlayers(): String {
        playerUpdate.players.forEach {
            "Id=${it.id} position=${it.playerMovement}".log()
        }
        return ""
    }

    override fun equals(other: Any?): Boolean {
        if (other !is WorldState) {
            "isn't WorldState".log()
            return false
        }

//        if (other.tick != this.tick) {
//            "tick are different other.tick=${other.tick}, this.tick=$tick".log()
//            return false
//        }

        if (other.playerUpdate.players.size != this.playerUpdate.players.size) {
            "size are different".log()
            return false
        }

        for (i in 0 until other.playerUpdate.players.size) {
            var found = false
            for (j in 0 until this.playerUpdate.players.size) {
                // find a player with the same ID
                if (other.playerUpdate.players[i].id == this.playerUpdate.players[j].id) {
                    found = true

                    // the player hasn't the same movement
                    if (other.playerUpdate.players[i].playerMovement != this.playerUpdate.players[j].playerMovement) {
                        "playerMovement are different SERVER=${this.playerUpdate.players[i].playerMovement}, CLIENT=${other.playerUpdate.players[i].playerMovement}".log()
                        return false
                    }

                    // the player hasn't the same aim
                    if (other.playerUpdate.players[i].playerAim != this.playerUpdate.players[j].playerAim) {
                        "playerAim are different".log()
                        return false
                    }

                    break
                }
            }

            // didn't find the player, it means that the list are different
            if (found.not()) {
                "didn't find".log()
                return false
            }
        }

//        "it's fine".log()
        return true
    }

    override fun hashCode(): Int {
        var result = tick
        result = 31 * result + playerUpdate.hashCode()
        result = 31 * result + (bulletUpdate?.hashCode() ?: 0)
        return result
    }
}

data class BulletUpdate(
    val bullets: List<Bullet>
)

data class PlayerUpdate(
    val players: List<PlayerServer>
)

data class PlayerServer(
    val id: String,
    val playerMovement: PlayerMovement,
    val playerAim: PlayerAim,
    val playerGunPointer: PlayerGunPointer,
)

data class PlayerGunPointer(
    val position: Position,
    val angle: Double,
)

data class PlayerAim(
    val angle: Double,
    val strength: Double,
) {
    fun wasAimApplied(): Boolean {
        return newPositionWasApplied.compareAndSet(false, true)
    }

    @Transient
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

    @Transient
    private val newPositionWasApplied = AtomicBoolean(false)
}

fun PlayerServer?.wasPositionMovementApplied(): Boolean {
    return this?.playerMovement?.wasPositionApplied() == true
}

fun PlayerServer?.wasAimApplied(): Boolean {
    return this?.playerAim?.wasAimApplied() == true
}