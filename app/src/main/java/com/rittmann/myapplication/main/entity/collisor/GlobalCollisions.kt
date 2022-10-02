package com.rittmann.myapplication.main.entity.collisor

object GlobalCollisions {
    val collidables: ArrayList<Collidable> = arrayListOf()

    fun add(collidable: Collidable) {
        collidables.add(collidable)
    }

    fun remove(collidable: Collidable) {
        collidables.remove(collidable)
    }

    fun verifyCollisions() {
        collidables.verifyCollisions()
    }
}