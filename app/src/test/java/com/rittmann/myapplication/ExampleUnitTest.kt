package com.rittmann.myapplication

import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        val BUFFER_SIZE = 1024

        val clientStatePreBuffer: Array<String?> = Array(BUFFER_SIZE) { null }

        clientStatePreBuffer[0 % BUFFER_SIZE] = "a"
        clientStatePreBuffer[1 % BUFFER_SIZE] = "b"
        clientStatePreBuffer[2 % BUFFER_SIZE] = "c"
        clientStatePreBuffer[3 % BUFFER_SIZE] = "d"

        assertEquals(null, clientStatePreBuffer[4 % BUFFER_SIZE])

    }
}