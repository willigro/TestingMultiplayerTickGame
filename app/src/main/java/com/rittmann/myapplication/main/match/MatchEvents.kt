package com.rittmann.myapplication.main.match

import com.rittmann.myapplication.main.entity.Bullet

interface MatchEvents {
    fun shoot(bullet: Bullet)
    fun update(deltaTime: Float)
    fun draw()
}