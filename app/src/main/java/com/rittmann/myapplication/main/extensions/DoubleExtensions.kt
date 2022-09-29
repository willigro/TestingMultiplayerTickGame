package com.rittmann.myapplication.main.extensions

fun Double?.orZero(): Double {
    return this ?: 0.0
}