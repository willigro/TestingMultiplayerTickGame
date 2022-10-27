package com.rittmann.myapplication.main.entity

sealed class MatchError(val id: String) {
    data class PlayerPositionError(val playerId: String) : MatchError(playerId)
    data class PlayerAngleError(val playerId: String) : MatchError(playerId)
    data class BulletPositionError(val bulletId: String) : MatchError(bulletId)
    data class BulletNotFoundError(val bulletId: String) : MatchError(bulletId)
}