package com.rittmann.myapplication.main.match

import com.rittmann.myapplication.main.entity.Bullet
import com.rittmann.myapplication.main.entity.Player

interface MatchEvents {
    fun shoot(player: Player, bullet: Bullet)
    fun update()
}