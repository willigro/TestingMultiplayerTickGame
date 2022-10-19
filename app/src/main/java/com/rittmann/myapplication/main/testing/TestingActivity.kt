package com.rittmann.myapplication.main.testing

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import com.rittmann.myapplication.R
import com.rittmann.myapplication.main.entity.Player
import com.rittmann.myapplication.main.entity.server.WorldState
import com.rittmann.myapplication.main.match.screen.MatchActivity
import com.rittmann.myapplication.main.match.screen.MatchController
import com.rittmann.myapplication.main.server.ConnectionControl
import com.rittmann.myapplication.main.server.ConnectionControlEvents
import com.rittmann.myapplication.main.server.ConnectionControlListeners
import com.rittmann.myapplication.main.utils.Logger
import kotlin.random.Random

class TestingActivity : AppCompatActivity(), ConnectionControlEvents, Logger {

    private var playerOne: Player? = null
    private var playerTwo: Player? = null

    private val matchController: MatchController by lazy {
        MatchController(
            ConnectionControl(connectionControlEvents = this)
        )
    }

    private val buttonOpenTheGame by lazy {
        findViewById<Button>(R.id.open_the_game)
    }

    private val buttonConnectOne by lazy {
        findViewById<Button>(R.id.connect_one)
    }

    private val buttonConnectTwo by lazy {
        findViewById<Button>(R.id.connect_two)
    }

    private val buttonEmitOne by lazy {
        findViewById<Button>(R.id.emit_one)
    }

    private val buttonEmitTwo by lazy {
        findViewById<Button>(R.id.emit_two)
    }

    private val buttonClear by lazy {
        findViewById<Button>(R.id.clear)
    }

    private val editMessageOne by lazy {
        findViewById<EditText>(R.id.message_one)
    }

    private val editMessageTwo by lazy {
        findViewById<EditText>(R.id.message_two)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_testing)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        buttonConnectOne.setOnClickListener {
            matchController.connect()
        }

        buttonConnectTwo.setOnClickListener {
            matchController.connect()
        }

        buttonEmitOne.setOnClickListener {
            matchController.emit(getString(R.string.default_emit_player_one_1))
            Thread.sleep(Random.nextLong(50, 120))
            matchController.emit(getString(R.string.default_emit_player_two_1))
            Thread.sleep(Random.nextLong(50, 120))
            matchController.emit(getString(R.string.default_emit_player_one_2))
            Thread.sleep(Random.nextLong(50, 120))
            matchController.emit(getString(R.string.default_emit_player_two_2))
            Thread.sleep(Random.nextLong(50, 120))
            matchController.emit(getString(R.string.default_emit_player_one_3))
            Thread.sleep(Random.nextLong(50, 120))
            matchController.emit(getString(R.string.default_emit_player_two_3))

            matchController.gameMustStop()
        }

        buttonEmitTwo.setOnClickListener {
//            matchController.emit(
//                editMessageTwo.text.toString()
//            )
        }

        buttonClear.setOnClickListener {
            editMessageOne.setText("")
            editMessageTwo.setText("")
        }

        buttonOpenTheGame.setOnClickListener {
            Intent(this, MatchActivity::class.java).apply {
                startActivity(this)
            }
        }
    }

    override fun logCallback(log: String) {
    }

    override fun connectionCreated(player: ConnectionControlListeners.NewPlayerConnected) {
        if (playerOne == null) {
            playerOne = player.player

            "Player ONE connected=$playerOne at tick=${player.tick}".log()
        } else if (playerTwo == null) {
            playerTwo = player.player

            "Player TWO connected=$playerTwo at tick=${player.tick}".log()
        }
    }

    override fun newPlayerConnected(player: ConnectionControlListeners.NewPlayerConnected) {
    }

    override fun onGameStarted(tick: Int) {
    }

    override fun onGameDisconnected() {

    }

    override fun playerDisconnected(id: String) {
    }

    override fun onPlayerUpdate(worldState: List<WorldState>) {
    }
}