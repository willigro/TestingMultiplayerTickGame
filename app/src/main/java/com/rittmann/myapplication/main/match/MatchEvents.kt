package com.rittmann.myapplication.main.match

import com.rittmann.myapplication.main.entity.Bullet
import com.rittmann.myapplication.main.entity.server.WorldState
import com.rittmann.myapplication.main.scene.SceneManager

interface MatchEvents {
    fun shoot(bullet: Bullet)
    fun sendTheUpdatedState(deltaTime: Double, tick: Int, worldState: WorldState?)
    fun sendTheUpdatedState(inputWorldState: SceneManager.InputWorldState)
    fun draw()
}