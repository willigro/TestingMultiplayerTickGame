package com.rittmann.myapplication.main.match

import com.rittmann.myapplication.main.entity.Bullet
import com.rittmann.myapplication.main.entity.server.InputWorldState
import com.rittmann.myapplication.main.entity.server.WorldState

interface MatchEvents {
    fun shoot(bullet: Bullet)
    fun sendTheUpdatedState(deltaTime: Double, tick: Int, worldState: WorldState?)
    fun sendTheUpdatedState(inputWorldState: InputWorldState)
    fun draw()
}