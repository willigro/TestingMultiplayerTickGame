package com.rittmann.myapplication.main.entity.collisor

/**
 * TODO: this collisions will be used only for graphic matter, since that who will really check
 *  and give the last word about the collision is the server
 * */
interface Collidable {
    fun retrieveCollider(): Collider
    fun collidingWith(collidable: Collidable)
}

fun List<Collidable>.verifyCollisions() {
    // Check all items
    for (i in 0 until this.size) {

        /*
        * I don't want to check the same combination, so I'm going to check always the current position
        * and the next position
        * Example:
        *   position 0 -> checks = X, 1, 2, 3, 4, ... 0 will be skip
        *   position 1 -> checks = X, X, 2, 3, 4, ... 0 and 1 will be skip, 0 - 1 already was checked
        *   position 2 -> checks = X, X, X, 3, 4, ... 0, 1 and 2 will be skip, 0 - 1, 0 - 2, 1 - 1, 1 - 2 already was checked
        *
        *   This way, I'll prevent checking the same combination twice
        */
        if (i + 1 <= this.lastIndex) {
            for (j in i + 1 until this.size) {
                val collidableOne = this[i]
                val collidableTwo = this[j]

                val colliderOne = collidableOne.retrieveCollider()
                val colliderTwo = collidableTwo.retrieveCollider()

                if (colliderOne.isColliding(colliderTwo)) {
                    // For preventing multiples calls when it's not necessary, the collider can be locked
                    // So it won't receive new notifications
                    if (colliderOne.isLocked().not()) {
                        collidableOne.collidingWith(collidableTwo)
                    }

                    // Same here
                    if (colliderTwo.isLocked().not()) {
                        collidableTwo.collidingWith(collidableOne)
                    }
                }
            }
        }
    }
}